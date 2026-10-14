package dev.rylex.nep.machine;

import net.neoforged.neoforge.items.ItemStackHandler;

public final class HandlerSnapshots {
    private HandlerSnapshots() {}

    public static ItemStackHandler of(ItemStackHandler source) {
        ItemStackHandler copy = new OverstackedItemHandler(source.getSlots());
        for (int slot = 0; slot < source.getSlots(); slot++) {
            copy.setStackInSlot(slot, source.getStackInSlot(slot).copy());
        }
        return copy;
    }
}
