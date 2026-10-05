package dev.rylex.nep.compat.ars;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public class ArcaneEnchantingMatrixCraftingMachine implements ICraftingMachine {

    private final ArcaneEnchantingMatrixBlockEntity matrix;

    public ArcaneEnchantingMatrixCraftingMachine(ArcaneEnchantingMatrixBlockEntity matrix) {
        this.matrix = matrix;
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        ItemStack icon = new ItemStack(NepArsContent.MATRIX_ITEM.get());
        return new PatternContainerGroup(
                AEItemKey.of(icon), NepArsContent.MATRIX.get().getName(), List.of());
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
