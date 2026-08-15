package dev.rylex.nep.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class OverstackedItemHandler extends ItemStackHandler {

    public static final int SLOT_LIMIT = 64;

    public OverstackedItemHandler(int size) {
        super(size);
    }

    @Override
    public int getSlotLimit(int slot) {
        return SLOT_LIMIT;
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        return getSlotLimit(slot);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0) {
            return ItemStack.EMPTY;
        }
        validateSlotIndex(slot);
        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int toExtract = Math.min(amount, getSlotLimit(slot));
        if (existing.getCount() <= toExtract) {
            if (simulate) {
                return existing.copy();
            }
            stacks.set(slot, ItemStack.EMPTY);
            onContentsChanged(slot);
            return existing;
        }
        if (!simulate) {
            stacks.set(slot, existing.copyWithCount(existing.getCount() - toExtract));
            onContentsChanged(slot);
        }
        return existing.copyWithCount(toExtract);
    }
}
