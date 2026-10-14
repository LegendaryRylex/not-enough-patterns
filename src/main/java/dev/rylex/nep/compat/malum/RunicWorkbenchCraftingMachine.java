package dev.rylex.nep.compat.malum;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEItems;
import com.sammy.malum.common.block.curiosities.runic_workbench.RunicWorkbenchBlockEntity;
import com.sammy.malum.registry.common.block.MalumBlocks;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.PushOutcome;
import dev.rylex.nep.pattern.RuneworkingPattern;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RunicWorkbenchCraftingMachine extends MemoizedPushMachine {

    private static final Memo MEMO = new Memo();

    private final RunicWorkbenchBlockEntity workbench;

    public RunicWorkbenchCraftingMachine(RunicWorkbenchBlockEntity workbench) {
        super(workbench, MEMO, "Runic Workbench");
        this.workbench = workbench;
    }

    static void clearCache() {
        MEMO.clear();
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        ItemStack icon = new ItemStack(MalumBlocks.RUNIC_WORKBENCH.get());
        return new PatternContainerGroup(
                AEItemKey.of(icon), MalumBlocks.RUNIC_WORKBENCH.get().getName(), List.of());
    }

    @Override
    boolean enabled() {
        return NepConfig.malumRuneworking();
    }

    @Override
    PushOutcome attempt(Level level, IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        if (!(patternDetails instanceof RuneworkingPattern)
                && patternDetails.getDefinition().getItem() != AEItems.PROCESSING_PATTERN.asItem()) {
            return PushOutcome.unsatisfiable("not a runeworking or processing pattern");
        }

        RuneworkingResolver.Plan plan = RuneworkingResolver.resolve(patternDetails, level);
        if (plan == null) {
            return PushOutcome.unsatisfiable("no runeworking recipe matched the pattern");
        }

        Map<AEItemKey, Long> provided = collectItems(inputs);
        if (provided == null) {
            return PushOutcome.unsatisfiable("runeworking takes items only");
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
        workbench.inventory.setStackInSlot(0, toStack(plan.primary()));
        ItemStack secondary = toStack(plan.secondary());

        SpiritReclaimer.expect(workbench, ejection);
        if (!workbench.tryCraft(level, workbench.inventory.getStackInSlot(0), secondary, true)) {
            workbench.inventory.clear();
            SpiritReclaimer.forget(workbench);
            return PushOutcome.retry("the workbench settled on no runeworking recipe once both inputs were loaded");
        }

        workbench.setChanged();
        return PushOutcome.accepted("shaping " + plan.result());
    }
}
