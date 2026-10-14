package dev.rylex.nep.provider;

import net.minecraft.world.inventory.Slot;

public final class ImportCardSlots {

    private ImportCardSlots() {}

    public static boolean isEmptyImportSlot(Slot slot) {
        return slot instanceof ImportCardSlot && !slot.hasItem();
    }
}
