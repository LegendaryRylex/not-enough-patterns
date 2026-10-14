package dev.rylex.nep.compat.occultism;

import java.util.function.BiPredicate;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

final class SpiritBookSlots extends ItemStacksResourceHandler {

    private final BiPredicate<Integer, ItemStack> validity;
    private final Runnable onChanged;

    SpiritBookSlots(int size, BiPredicate<Integer, ItemStack> validity, Runnable onChanged) {
        super(size);
        this.validity = validity;
        this.onChanged = onChanged;
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        return 1;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return validity.test(index, resource.toStack());
    }

    @Override
    protected void onContentsChanged(int index, ItemStack previousContents) {
        onChanged.run();
    }

    ItemStack stackAt(int index) {
        return getResource(index).toStack(getAmountAsInt(index));
    }

    void put(int index, ItemStack stack) {
        if (stack.isEmpty()) {
            set(index, ItemResource.EMPTY, 0);
        } else {
            set(index, ItemResource.of(stack), 1);
        }
    }
}
