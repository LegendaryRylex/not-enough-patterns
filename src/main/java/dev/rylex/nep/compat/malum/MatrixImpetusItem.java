package dev.rylex.nep.compat.malum;

import dev.rylex.nep.NepConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class MatrixImpetusItem extends Item {

    MatrixImpetusItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return NepConfig.malumFocusedSpiritMatrixImpetusDurability();
    }
}
