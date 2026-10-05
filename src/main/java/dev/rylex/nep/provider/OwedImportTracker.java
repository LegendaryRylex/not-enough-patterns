package dev.rylex.nep.provider;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import dev.rylex.nep.NepConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

public final class OwedImportTracker {

    private static final String DISPATCHES_KEY = "nepOwedDispatches";
    private static final String LEGACY_KEY = "nepOwed";

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
        Map<AEKey, Long> owed = new LinkedHashMap<>();
        for (var output : pattern.getOutputs()) {
            if (output.amount() > 0 && output.what() instanceof AEKey key) {
                owed.merge(key, output.amount(), Long::sum);
            }
        }
        if (owed.isEmpty()) {
            return false;
        }
        dispatches.computeIfAbsent(source, s -> new ArrayList<>()).add(new OwedDispatch(parkedItems(inputs), owed));
        return true;
    }

    private static Map<AEKey, Long> parkedItems(@Nullable KeyCounter[] inputs) {
        Map<AEKey, Long> parked = new LinkedHashMap<>();
        if (inputs == null) {
            return parked;
        }
        for (KeyCounter counter : inputs) {
            if (counter == null) {
                continue;
            }
            for (var entry : counter) {
                if (entry.getLongValue() > 0 && entry.getKey() instanceof AEKey key) {
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

            for (Map.Entry<AEKey, Long> budgeted : budgets(live).entrySet()) {
                AEKey key = budgeted.getKey();
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

    private static Map<AEKey, Long> budgets(List<OwedDispatch> live) {
        Map<AEKey, Long> owed = new LinkedHashMap<>();
        Map<AEKey, Long> reserved = new HashMap<>();
        for (OwedDispatch dispatch : live) {
            for (Map.Entry<AEKey, Long> entry : dispatch.owed().entrySet()) {
                owed.merge(entry.getKey(), entry.getValue(), Long::sum);
            }
            for (AEKey key : dispatch.parked().keySet()) {
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

    private static void settle(List<OwedDispatch> live, AEKey key, long imported) {
        long remaining = imported;
        for (OwedDispatch dispatch : live) {
            if (remaining <= 0) {
                return;
            }
            remaining -= dispatch.take(key, remaining);
        }
    }

    private static boolean isRequested(ICraftingService crafting, OwedDispatch dispatch) {
        for (AEKey key : dispatch.owed().keySet()) {
            if (crafting.getRequestedAmount(key) > 0) {
                return true;
            }
        }
        return false;
    }

    public void writeToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag();
        for (Map.Entry<OwedSource, List<OwedDispatch>> sourceEntry : dispatches.entrySet()) {
            for (OwedDispatch dispatch : sourceEntry.getValue()) {
                var entryTag = new CompoundTag();
                sourceEntry.getKey().writeToNBT(entryTag);
                entryTag.put("owed", writeStacks(registries, dispatch.owed()));
                if (!dispatch.parked().isEmpty()) {
                    entryTag.put("parked", writeStacks(registries, dispatch.parked()));
                }
                if (dispatch.hasDeadline()) {
                    entryTag.putLong("grace", dispatch.deadline());
                }
                list.add(entryTag);
            }
        }
        tag.put(DISPATCHES_KEY, list);
    }

    public void readFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        clear();

        if (tag.contains(DISPATCHES_KEY, Tag.TAG_LIST)) {
            var list = tag.getList(DISPATCHES_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                var entryTag = list.getCompound(i);
                Map<AEKey, Long> owed = new LinkedHashMap<>();
                readStacks(registries, entryTag.getList("owed", Tag.TAG_COMPOUND), owed);
                if (owed.isEmpty()) {
                    continue;
                }
                Map<AEKey, Long> parked = new LinkedHashMap<>();
                readStacks(registries, entryTag.getList("parked", Tag.TAG_COMPOUND), parked);
                long deadline = entryTag.contains("grace") ? entryTag.getLong("grace") : OwedDispatch.NO_DEADLINE;
                add(OwedSource.readFromNBT(entryTag), new OwedDispatch(parked, owed, deadline));
            }
            return;
        }

        var legacy = tag.getList(LEGACY_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < legacy.size(); i++) {
            var entryTag = legacy.getCompound(i);
            var stack = GenericStack.readTag(registries, entryTag.getCompound("stack"));
            if (stack == null || stack.amount() <= 0 || !(stack.what() instanceof AEKey key)) {
                continue;
            }
            long deadline = entryTag.contains("grace") ? entryTag.getLong("grace") : OwedDispatch.NO_DEADLINE;
            add(
                    OwedSource.readFromNBT(entryTag),
                    new OwedDispatch(Map.of(), Map.of(stack.what(), stack.amount()), deadline));
        }
    }

    private void add(OwedSource source, OwedDispatch dispatch) {
        dispatches.computeIfAbsent(source, s -> new ArrayList<>()).add(dispatch);
    }

    private static ListTag writeStacks(HolderLookup.Provider registries, Map<AEKey, Long> stacks) {
        var list = new ListTag();
        for (Map.Entry<AEKey, Long> entry : stacks.entrySet()) {
            list.add(GenericStack.writeTag(registries, new GenericStack(entry.getKey(), entry.getValue())));
        }
        return list;
    }

    private static void readStacks(HolderLookup.Provider registries, ListTag list, Map<AEKey, Long> into) {
        for (int i = 0; i < list.size(); i++) {
            var stack = GenericStack.readTag(registries, list.getCompound(i));
            if (stack != null && stack.amount() > 0 && stack.what() instanceof AEKey key) {
                into.merge(key, stack.amount(), Long::sum);
            }
        }
    }

    private StackImportStrategy strategyFor(ServerLevel level, BlockPos providerPos, OwedSource source) {
        return strategies.computeIfAbsent(source, s -> s.createStrategy(level, providerPos));
    }
}
