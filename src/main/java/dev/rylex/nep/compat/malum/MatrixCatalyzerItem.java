package dev.rylex.nep.compat.malum;

import dev.rylex.nep.NepConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class MatrixCatalyzerItem extends Item {

    private static final int FULL_STACK = 64;

    MatrixCatalyzerItem(Properties properties) {
        super(properties);
    }

    static int installLimit() {
        return Math.min(FULL_STACK, NepConfig.malumFocusedSpiritMatrixMaxCatalyzers());
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return installLimit();
    }
}
