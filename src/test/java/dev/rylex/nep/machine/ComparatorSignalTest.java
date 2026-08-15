package dev.rylex.nep.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.rylex.nep.MinecraftBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrap.class)
class ComparatorSignalTest {

    @Test
    void emptyBufferReadsZeroAndAnySingleItemReadsAtLeastOne() {
        assertEquals(0, ComparatorSignal.of(new ItemStacksResourceHandler(9)));

        ItemStacksResourceHandler one = new ItemStacksResourceHandler(9);
        put(one, 0, new ItemStack(Items.COBBLESTONE, 1));
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
        assertEquals(0, ComparatorSignal.of(new ItemStacksResourceHandler(0)));
    }

    @Test
    void unstackableItemFillsItsSlotCompletely() {
        ItemStacksResourceHandler handler = new ItemStacksResourceHandler(1);
        put(handler, 0, new ItemStack(Items.DIAMOND_PICKAXE));
        assertEquals(15, ComparatorSignal.of(handler));
    }

    @Test
    void aSlotOverstackedPastItsItemsLimitStillCountsAsOneSlot() {
        MatrixBuffer buffer = new MatrixBuffer(2, () -> {});
        put(buffer, 0, new ItemStack(Items.ENDER_PEARL, MatrixBuffer.SLOT_LIMIT));
        assertEquals(scale(0.5F), ComparatorSignal.of(buffer));
    }

    private static int scale(float fill) {
        return (int) Math.floor(fill * 14.0F) + (fill > 0.0F ? 1 : 0);
    }

    private static void put(ItemStacksResourceHandler handler, int slot, ItemStack stack) {
        handler.set(slot, ItemResource.of(stack), stack.getCount());
    }

    private static ItemStacksResourceHandler stacks(int slots, int fullSlots) {
        ItemStacksResourceHandler handler = new ItemStacksResourceHandler(slots);
        for (int slot = 0; slot < fullSlots; slot++) {
            put(handler, slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        return handler;
    }
}
