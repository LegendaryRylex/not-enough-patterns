package dev.rylex.nep.compat.oritech;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

final class MatrixCoreSlot extends ItemStacksResourceHandler {

    private final Runnable onChanged;

    MatrixCoreSlot(Runnable onChanged) {
        super(1);
        this.onChanged = onChanged;
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        return 1;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return MatrixAddons.isCore(resource.toStack());
    }

    @Override
    protected void onContentsChanged(int index, ItemStack previousContents) {
        onChanged.run();
    }

    ItemStack stack() {
        return getResource(0).toStack(getAmountAsInt(0));
    }

    void put(ItemStack stack) {
        if (stack.isEmpty()) {
            set(0, ItemResource.EMPTY, 0);
        } else {
            set(0, ItemResource.of(stack), 1);
        }
    }
}
