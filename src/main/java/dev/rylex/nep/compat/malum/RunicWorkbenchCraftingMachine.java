package dev.rylex.nep.compat.malum;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEItems;
import com.sammy.malum.common.block.curiosities.runic_workbench.RunicWorkbenchBlockEntity;
import com.sammy.malum.registry.common.block.MalumBlocks;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.PushOutcome;
import dev.rylex.nep.machine.RejectLog;
import dev.rylex.nep.pattern.RuneworkingPattern;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RunicWorkbenchCraftingMachine implements ICraftingMachine {

    private static final int MAX_MEMOIZED_REJECTS = 512;

    private static final Set<AEItemKey> UNSATISFIABLE = new HashSet<>();
    private static final RejectLog REJECT_LOG = new RejectLog();

    private final RunicWorkbenchBlockEntity workbench;

    public RunicWorkbenchCraftingMachine(RunicWorkbenchBlockEntity workbench) {
        this.workbench = workbench;
    }

    static void clearCache() {
        UNSATISFIABLE.clear();
        REJECT_LOG.clear();
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        ItemStack icon = new ItemStack(MalumBlocks.RUNIC_WORKBENCH.get());
        return new PatternContainerGroup(
                AEItemKey.of(icon), MalumBlocks.RUNIC_WORKBENCH.get().getName(), List.of());
    }

    @Override
    public boolean acceptsPlans() {
        return NepConfig.malumRuneworking();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = workbench.getLevel();
        if (level == null || level.isClientSide() || !NepConfig.malumRuneworking()) {
            return false;
        }

        AEItemKey key = patternDetails.getDefinition();
        if (UNSATISFIABLE.contains(key)) {
            return false;
        }

        PushOutcome outcome = attempt(level, patternDetails, inputs, ejectionDirection);
        if (outcome.unsatisfiable() && UNSATISFIABLE.size() < MAX_MEMOIZED_REJECTS) {
            UNSATISFIABLE.add(key);
        }
        log(level, patternDetails, outcome);
        return outcome.accepted();
    }

    private PushOutcome attempt(
            Level level, IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        if (!(patternDetails instanceof RuneworkingPattern)
                && patternDetails.getDefinition().getItem() != AEItems.PROCESSING_PATTERN.asItem()) {
            return PushOutcome.unsatisfiable("not a runeworking or processing pattern");
        }

        RuneworkingResolver.Plan plan = RuneworkingResolver.resolve(patternDetails, level);
        if (plan == null) {
            return PushOutcome.unsatisfiable("no runeworking recipe matched the pattern");
        }

        Map<AEItemKey, Long> provided = new HashMap<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return PushOutcome.unsatisfiable("runeworking takes items only");
                }
                provided.merge(itemKey, entry.getLongValue(), Long::sum);
            }
        }
        if (!provided.equals(plan.expectedItems())) {
            return PushOutcome.unsatisfiable(
                    "pushed items " + provided + " do not match the recipe's inputs " + plan.expectedItems());
        }

        if (plan.primary().amount() > workbench.inventory.getSlotLimit(0)) {
            return PushOutcome.unsatisfiable("the workbench slot cannot hold " + plan.primary());
        }

        if (!workbench.inventory.getStackInSlot(0).isEmpty()) {
            return PushOutcome.retry("workbench is already holding an item");
        }
        if (((RunicWorkbenchState) workbench).nep$isCrafting()) {
            return PushOutcome.retry("workbench is still shaping the last rune");
        }

        return commit(level, plan, ejectionDirection);
    }

    private PushOutcome commit(Level level, RuneworkingResolver.Plan plan, Direction ejection) {
        workbench.inventory.setStackInSlot(0, SpiritAltarCraftingMachine.toStack(plan.primary()));
        ItemStack secondary = SpiritAltarCraftingMachine.toStack(plan.secondary());

        SpiritReclaimer.expect(workbench, ejection);
        if (!workbench.tryCraft(level, workbench.inventory.getStackInSlot(0), secondary, true)) {
            workbench.inventory.clear();
            SpiritReclaimer.forget(workbench);
            return PushOutcome.retry("the workbench settled on no runeworking recipe once both inputs were loaded");
        }

        workbench.setChanged();
        return PushOutcome.accepted("shaping " + plan.result());
    }

    private void log(Level level, IPatternDetails patternDetails, PushOutcome outcome) {
        if (!NepConfig.debugLogging()) {
            return;
        }
        if (!outcome.accepted()
                && !outcome.unsatisfiable()
                && !REJECT_LOG.shouldLog(level, workbench.getBlockPos(), outcome.detail())) {
            return;
        }
        Nep.LOGGER.info(
                "Runic Workbench {} {} push: {} (output={})",
                workbench.getBlockPos(),
                outcome.accepted() ? "accepted" : "rejected",
                outcome.detail(),
                patternDetails.getOutputs().isEmpty()
                        ? "?"
                        : patternDetails.getOutputs().get(0).toString());
    }
}
