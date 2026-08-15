package dev.rylex.nep.provider;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.mojang.serialization.Codec;
import dev.rylex.nep.NepConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public final class OwedImportTracker {

    private static final String DISPATCHES_KEY = "nepOwedDispatches";
    private static final String LEGACY_KEY = "nepOwed";

    private static final Codec<List<GenericStack>> STACKS_CODEC = GenericStack.CODEC.listOf();

    public record ImportResult(boolean moved, boolean changed) {}

    private final Map<OwedSource, List<OwedDispatch>> dispatches = new LinkedHashMap<>();
    private final Map<OwedSource, StackImportStrategy> strategies = new HashMap<>();

    public boolean isEmpty() {
        return dispatches.isEmpty();
    }

    public void clear() {
        dispatches.clear();
        strategies.clear();
    }

    public boolean record(IPatternDetails pattern, @Nullable KeyCounter[] inputs, OwedSource source) {
        Map<AEItemKey, Long> owed = new LinkedHashMap<>();
        for (var output : pattern.getOutputs()) {
            if (output.amount() > 0 && output.what() instanceof AEItemKey key) {
                owed.merge(key, output.amount(), Long::sum);
            }
        }
        if (owed.isEmpty()) {
            return false;
        }
        dispatches.computeIfAbsent(source, s -> new ArrayList<>()).add(new OwedDispatch(parkedItems(inputs), owed));
        return true;
    }

    private static Map<AEItemKey, Long> parkedItems(@Nullable KeyCounter[] inputs) {
        Map<AEItemKey, Long> parked = new LinkedHashMap<>();
        if (inputs == null) {
            return parked;
        }
        for (KeyCounter counter : inputs) {
            if (counter == null) {
                continue;
            }
            for (var entry : counter) {
                if (entry.getLongValue() > 0 && entry.getKey() instanceof AEItemKey key) {
                    parked.merge(key, entry.getLongValue(), Long::sum);
                }
            }
        }
        return parked;
    }

    public ImportResult importOwed(ServerLevel level, BlockPos providerPos, IGrid grid, IActionSource actionSource) {
        ICraftingService crafting = grid.getCraftingService();
        var storage = grid.getStorageService();
        var energy = grid.getEnergyService();

        long now = level.getGameTime();
        long grace = NepConfig.importCardGrace();

        boolean moved = false;
        boolean changed = false;
        Iterator<Map.Entry<OwedSource, List<OwedDispatch>>> sources =
                dispatches.entrySet().iterator();
        while (sources.hasNext()) {
            Map.Entry<OwedSource, List<OwedDispatch>> sourceEntry = sources.next();
            OwedSource source = sourceEntry.getKey();
            List<OwedDispatch> live = sourceEntry.getValue();

            for (Map.Entry<AEItemKey, Long> budgeted : budgets(live).entrySet()) {
                AEItemKey key = budgeted.getKey();
                var context = new OwedImportContext(
                        storage, energy, actionSource, key, (int) Math.min(budgeted.getValue(), Integer.MAX_VALUE));
                strategyFor(level, providerPos, source).transfer(context);

                long imported = context.moved();
                if (imported > 0) {
                    moved = true;
                    changed = true;
                    settle(live, key, imported);
                }
            }

            Iterator<OwedDispatch> pending = live.iterator();
            while (pending.hasNext()) {
                OwedDispatch dispatch = pending.next();
                if (dispatch.isSettled()) {
                    pending.remove();
                    changed = true;
                    continue;
                }
                boolean requested = isRequested(crafting, dispatch);
                if (requested) {
                    dispatch.clearGrace();
                } else if (!dispatch.hasDeadline()) {
                    dispatch.startGrace(now + grace);
                }
                if (!requested && dispatch.expired(now)) {
                    pending.remove();
                    changed = true;
                }
            }

            if (live.isEmpty()) {
                strategies.remove(source);
                sources.remove();
            }
        }

        return new ImportResult(moved, changed);
    }

    private static Map<AEItemKey, Long> budgets(List<OwedDispatch> live) {
        Map<AEItemKey, Long> owed = new LinkedHashMap<>();
        Map<AEItemKey, Long> reserved = new HashMap<>();
        for (OwedDispatch dispatch : live) {
            for (Map.Entry<AEItemKey, Long> entry : dispatch.owed().entrySet()) {
                owed.merge(entry.getKey(), entry.getValue(), Long::sum);
            }
            for (AEItemKey key : dispatch.parked().keySet()) {
                long amount = dispatch.reserved(key);
                if (amount > 0) {
                    reserved.merge(key, amount, Long::sum);
                }
            }
        }
        owed.entrySet().removeIf(entry -> {
            long budget = entry.getValue() - reserved.getOrDefault(entry.getKey(), 0L);
            entry.setValue(budget);
            return budget <= 0;
        });
        return owed;
    }

    private static void settle(List<OwedDispatch> live, AEItemKey key, long imported) {
        long remaining = imported;
        for (OwedDispatch dispatch : live) {
            if (remaining <= 0) {
                return;
            }
            remaining -= dispatch.take(key, remaining);
        }
    }

    private static boolean isRequested(ICraftingService crafting, OwedDispatch dispatch) {
        for (AEItemKey key : dispatch.owed().keySet()) {
            if (crafting.getRequestedAmount(key) > 0) {
                return true;
            }
        }
        return false;
    }

    public void writeToNBT(ValueOutput output) {
        var list = output.childrenList(DISPATCHES_KEY);
        for (Map.Entry<OwedSource, List<OwedDispatch>> sourceEntry : dispatches.entrySet()) {
            for (OwedDispatch dispatch : sourceEntry.getValue()) {
                var entryOutput = list.addChild();
                sourceEntry.getKey().writeToNBT(entryOutput);
                entryOutput.store("owed", STACKS_CODEC, stacksOf(dispatch.owed()));
                if (!dispatch.parked().isEmpty()) {
                    entryOutput.store("parked", STACKS_CODEC, stacksOf(dispatch.parked()));
                }
                if (dispatch.hasDeadline()) {
                    entryOutput.putLong("grace", dispatch.deadline());
                }
            }
        }
    }

    public void readFromNBT(ValueInput input) {
        clear();

        for (var entryInput : input.childrenListOrEmpty(DISPATCHES_KEY)) {
            Map<AEItemKey, Long> owed =
                    stacksInto(entryInput.read("owed", STACKS_CODEC).orElse(List.of()));
            if (owed.isEmpty()) {
                continue;
            }
            Map<AEItemKey, Long> parked =
                    stacksInto(entryInput.read("parked", STACKS_CODEC).orElse(List.of()));
            long deadline = entryInput.getLongOr("grace", OwedDispatch.NO_DEADLINE);
            add(OwedSource.readFromNBT(entryInput), new OwedDispatch(parked, owed, deadline));
        }
        if (!dispatches.isEmpty()) {
            return;
        }

        for (var entryInput : input.childrenListOrEmpty(LEGACY_KEY)) {
            var stack = entryInput.read("stack", GenericStack.CODEC).orElse(null);
            if (stack == null || stack.amount() <= 0 || !(stack.what() instanceof AEItemKey key)) {
                continue;
            }
            long deadline = entryInput.getLongOr("grace", OwedDispatch.NO_DEADLINE);
            add(OwedSource.readFromNBT(entryInput), new OwedDispatch(Map.of(), Map.of(key, stack.amount()), deadline));
        }
    }

    private void add(OwedSource source, OwedDispatch dispatch) {
        dispatches.computeIfAbsent(source, s -> new ArrayList<>()).add(dispatch);
    }

    private static List<GenericStack> stacksOf(Map<AEItemKey, Long> stacks) {
        List<GenericStack> list = new ArrayList<>(stacks.size());
        for (Map.Entry<AEItemKey, Long> entry : stacks.entrySet()) {
            list.add(new GenericStack(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    private static Map<AEItemKey, Long> stacksInto(List<GenericStack> stacks) {
        Map<AEItemKey, Long> into = new LinkedHashMap<>();
        for (GenericStack stack : stacks) {
            if (stack != null && stack.amount() > 0 && stack.what() instanceof AEItemKey key) {
                into.merge(key, stack.amount(), Long::sum);
            }
        }
        return into;
    }

    private StackImportStrategy strategyFor(ServerLevel level, BlockPos providerPos, OwedSource source) {
        return strategies.computeIfAbsent(source, s -> s.createStrategy(level, providerPos));
    }
}
