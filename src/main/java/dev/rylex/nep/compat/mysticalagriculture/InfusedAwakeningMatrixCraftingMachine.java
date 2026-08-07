package dev.rylex.nep.compat.mysticalagriculture;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public class InfusedAwakeningMatrixCraftingMachine implements ICraftingMachine {

    private final InfusedAwakeningMatrixBlockEntity matrix;

    public InfusedAwakeningMatrixCraftingMachine(InfusedAwakeningMatrixBlockEntity matrix) {
        this.matrix = matrix;
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        return new PatternContainerGroup(
                AEItemKey.of(new ItemStack(NepMysticalContent.MATRIX_ITEM.get())),
                NepMysticalContent.MATRIX.get().getName(),
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
