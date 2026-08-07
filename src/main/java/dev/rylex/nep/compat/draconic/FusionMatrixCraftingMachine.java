package dev.rylex.nep.compat.draconic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public class FusionMatrixCraftingMachine implements ICraftingMachine {

    private final FusionMatrixBlockEntity matrix;

    public FusionMatrixCraftingMachine(FusionMatrixBlockEntity matrix) {
        this.matrix = matrix;
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        return new PatternContainerGroup(
                AEItemKey.of(new ItemStack(NepDraconicContent.MATRIX_ITEM.get())),
                NepDraconicContent.MATRIX.get().getName(),
                List.of());
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
