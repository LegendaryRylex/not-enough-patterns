package dev.rylex.nep.compat.occultism;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public class BoundSpiritMatrixCraftingMachine implements ICraftingMachine {

    private final BoundSpiritMatrixBlockEntity matrix;

    public BoundSpiritMatrixCraftingMachine(BoundSpiritMatrixBlockEntity matrix) {
        this.matrix = matrix;
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        ItemStack icon = new ItemStack(NepOccultismContent.MATRIX_ITEM.get());
        return new PatternContainerGroup(
                AEItemKey.of(icon), NepOccultismContent.MATRIX.get().getName(), List.of());
    }

    @Override
    public boolean acceptsPlans() {
        return matrix.readyForPatterns();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        return matrix.pushMatrixPattern(patternDetails, inputs, ejectionDirection);
    }
}
