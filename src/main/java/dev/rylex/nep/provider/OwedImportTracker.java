package dev.rylex.nep.provider;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.NepConfig;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class OwedImportTracker {

    public record ImportResult(boolean moved, boolean changed) {}

    private final Map<OwedSource, Map<AEItemKey, OwedOutput>> owed = new LinkedHashMap<>();
    private final Map<OwedSource, StackImportStrategy> strategies = new HashMap<>();

    public boolean isEmpty() {
        return owed.isEmpty();
    }

    public void clear() {
        owed.clear();
        strategies.clear();
    }

    public boolean record(IPatternDetails pattern, OwedSource source) {
        boolean recorded = false;
        for (var output : pattern.getOutputs()) {
            if (output.amount() > 0 && output.what() instanceof AEItemKey key) {
                owed.computeIfAbsent(source, s -> new LinkedHashMap<>()).compute(key, (k, existing) -> {
                    if (existing == null) {
                        return new OwedOutput(output.amount());
                    }
                    existing.add(output.amount());
                    return existing;
                });
                recorded = true;
            }
        }
        return recorded;
    }

    public ImportResult importOwed(ServerLevel level, BlockPos providerPos, IGrid grid, IActionSource actionSource) {
        var crafting = grid.getCraftingService();
        var storage = grid.getStorageService();
        var energy = grid.getEnergyService();

        long now = level.getGameTime();
        long grace = NepConfig.importCardGrace();

        boolean moved = false;
        boolean changed = false;
        var sources = owed.entrySet().iterator();
        while (sources.hasNext()) {
            var sourceEntry = sources.next();
            var source = sourceEntry.getKey();
            var entries = sourceEntry.getValue().entrySet().iterator();

            while (entries.hasNext()) {
                var entry = entries.next();
                var key = entry.getKey();
                var owedOutput = entry.getValue();

                boolean requested = crafting.getRequestedAmount(key) > 0;
                if (requested) {
                    owedOutput.clearGrace();
                } else if (!owedOutput.hasDeadline()) {
                    owedOutput.startGrace(now + grace);
                }

                long before = storage.getInventory().extract(key, Long.MAX_VALUE, Actionable.SIMULATE, actionSource);
                var context = new OwedImportContext(
                        storage, energy, actionSource, key, (int) Math.min(owedOutput.amount(), Integer.MAX_VALUE));
                strategyFor(level, providerPos, source).transfer(context);

                long after = storage.getInventory().extract(key, Long.MAX_VALUE, Actionable.SIMULATE, actionSource);
                long imported = Math.max(0, after - before);
                if (imported > 0) {
                    moved = true;
                    changed = true;
                    if (owedOutput.take(imported)) {
                        entries.remove();
                    } else if (!requested) {
                        owedOutput.startGrace(now + grace);
                    }
                } else if (owedOutput.expired(now)) {
                    changed = true;
                    entries.remove();
                }
            }

            if (sourceEntry.getValue().isEmpty()) {
                strategies.remove(source);
                sources.remove();
            }
        }

        return new ImportResult(moved, changed);
    }

    public void writeToNBT(ValueOutput output) {
        var owedList = output.childrenList("nepOwed");
        for (var sourceEntry : owed.entrySet()) {
            for (var entry : sourceEntry.getValue().entrySet()) {
                var owedOutput = entry.getValue();
                var entryOutput = owedList.addChild();
                sourceEntry.getKey().writeToNBT(entryOutput);
                entryOutput.store("stack", GenericStack.CODEC, new GenericStack(entry.getKey(), owedOutput.amount()));
                if (owedOutput.hasDeadline()) {
                    entryOutput.putLong("grace", owedOutput.deadline());
                }
            }
        }
    }

    public void readFromNBT(ValueInput input) {
        clear();

        for (var entryInput : input.childrenListOrEmpty("nepOwed")) {
            var stack = entryInput.read("stack", GenericStack.CODEC).orElse(null);
            if (stack == null || stack.amount() <= 0 || !(stack.what() instanceof AEItemKey key)) {
                continue;
            }
            long deadline = entryInput.getLongOr("grace", OwedOutput.NO_DEADLINE);
            var bySource = owed.computeIfAbsent(OwedSource.readFromNBT(entryInput), s -> new LinkedHashMap<>());
            var existing = bySource.get(key);
            if (existing != null) {
                existing.add(stack.amount());
            } else {
                bySource.put(key, new OwedOutput(stack.amount(), deadline));
            }
        }
    }

    private StackImportStrategy strategyFor(ServerLevel level, BlockPos providerPos, OwedSource source) {
        return strategies.computeIfAbsent(source, s -> s.createStrategy(level, providerPos));
    }
}
