package dev.rylex.nep.provider;

import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.RestrictedInputSlot;
import dev.rylex.nep.NepItems;
import net.minecraft.world.item.ItemStack;

public final class ImportCardSlot extends RestrictedInputSlot {

    public ImportCardSlot(InternalInventory inv, int invSlot) {
        super(PlacableItemType.UPGRADES, inv, invSlot);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(NepItems.IMPORT_CARD.get()) && super.mayPlace(stack);
    }
}
