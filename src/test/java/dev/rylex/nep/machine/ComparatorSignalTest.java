package dev.rylex.nep.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

class ComparatorSignalTest {

    @Test
    void emptyBufferReadsZeroAndAnySingleItemReadsAtLeastOne() {
        assertEquals(0, ComparatorSignal.of(new ItemStackHandler(9)));

        ItemStackHandler one = new ItemStackHandler(9);
        one.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 1));
        assertEquals(1, ComparatorSignal.of(one));
    }

    @Test
    void fullBufferReadsFifteen() {
        assertEquals(15, ComparatorSignal.of(stacks(9, 9)));
    }

    @Test
    void fullnessMatchesTheVanillaContainerFormula() {
        assertEquals(scale(5.0F / 9.0F), ComparatorSignal.of(stacks(9, 5)));
        assertEquals(scale(2.0F / 9.0F), ComparatorSignal.of(stacks(9, 2)));
    }

    @Test
    void zeroSlotHandlerReadsZeroInsteadOfDividingByZero() {
        assertEquals(0, ComparatorSignal.of(new ItemStackHandler(0)));
    }

    @Test
    void unstackableItemFillsItsSlotCompletely() {
        ItemStackHandler handler = new ItemStackHandler(1);
        handler.setStackInSlot(0, new ItemStack(Items.DIAMOND_PICKAXE));
        assertEquals(15, ComparatorSignal.of(handler));
    }

    @Test
    void aSlotOverstackedPastItsItemsLimitStillCountsAsOneSlot() {
        ItemStackHandler handler = new OverstackedItemHandler(2);
        handler.setStackInSlot(0, new ItemStack(Items.ENDER_PEARL, OverstackedItemHandler.SLOT_LIMIT));
        assertEquals(scale(0.5F), ComparatorSignal.of(handler));
    }

    private static int scale(float fill) {
        return (int) Math.floor(fill * 14.0F) + (fill > 0.0F ? 1 : 0);
    }

    private static ItemStackHandler stacks(int slots, int fullSlots) {
        ItemStackHandler handler = new ItemStackHandler(slots);
        for (int slot = 0; slot < fullSlots; slot++) {
            handler.setStackInSlot(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        return handler;
    }
}
