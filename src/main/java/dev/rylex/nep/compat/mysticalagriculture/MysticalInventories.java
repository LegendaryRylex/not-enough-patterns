package dev.rylex.nep.compat.mysticalagriculture;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

final class MysticalInventories {
    private MysticalInventories() {}

    static boolean isEmpty(ResourceHandler<ItemResource> inventory, int slot) {
        return inventory.getResource(slot).isEmpty() || inventory.getAmountAsLong(slot) <= 0;
    }

    static void set(ItemStacksResourceHandler inventory, int slot, ItemStack stack) {
        inventory.set(slot, ItemResource.of(stack), stack.getCount());
    }
}
