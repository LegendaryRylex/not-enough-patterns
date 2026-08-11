package dev.rylex.nep.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.rylex.nep.MinecraftBootstrap;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrap.class)
class RedstoneModeTest {

    @Test
    void cyclesThroughEveryModeAndWrapsAround() {
        assertSame(RedstoneMode.STATUS, RedstoneMode.OUTPUT.next());
        assertSame(RedstoneMode.INPUT, RedstoneMode.STATUS.next());
        assertSame(RedstoneMode.OUTPUT, RedstoneMode.INPUT.next());
    }

    @Test
    void everyModeHasADistinctGlyphAndItsOwnLangKey() {
        assertEquals("O", RedstoneMode.OUTPUT.glyph());
        assertEquals("S", RedstoneMode.STATUS.glyph());
        assertEquals("I", RedstoneMode.INPUT.glyph());

        assertEquals("gui.nep.redstone_mode.output", RedstoneMode.OUTPUT.key());
        assertEquals("gui.nep.redstone_mode.status", RedstoneMode.STATUS.key());
        assertEquals("gui.nep.redstone_mode.input", RedstoneMode.INPUT.key());

        assertNotEquals(RedstoneMode.OUTPUT.key(), RedstoneMode.STATUS.key());
    }

    @Test
    void decodingFallsBackToOutputRatherThanThrowing() {
        assertSame(RedstoneMode.STATUS, RedstoneMode.byOrdinal(1));
        assertSame(RedstoneMode.OUTPUT, RedstoneMode.byOrdinal(-1));
        assertSame(RedstoneMode.OUTPUT, RedstoneMode.byOrdinal(99));

        assertSame(RedstoneMode.INPUT, RedstoneMode.byName("INPUT"));
        assertSame(RedstoneMode.OUTPUT, RedstoneMode.byName("input"));
        assertSame(RedstoneMode.OUTPUT, RedstoneMode.byName("nonsense"));
    }

    @Test
    void ordinalAndNameRoundTripThroughPayloadAndNbt() {
        for (RedstoneMode mode : RedstoneMode.values()) {
            assertSame(mode, RedstoneMode.byOrdinal(mode.ordinal()));
            assertSame(mode, RedstoneMode.byName(mode.name()));
        }
    }

    @Test
    void emptyBufferReadsZeroAndAnySingleItemReadsAtLeastOne() {
        assertEquals(0, RedstoneMode.fullness(new ItemStacksResourceHandler(9)));

        ItemStacksResourceHandler one = new ItemStacksResourceHandler(9);
        one.set(0, ItemResource.of(Items.COBBLESTONE), 1);
        assertEquals(1, RedstoneMode.fullness(one));
    }

    @Test
    void fullBufferReadsFifteen() {
        assertEquals(15, RedstoneMode.fullness(stacks(9, 9)));
    }

    @Test
    void fullnessMatchesTheVanillaContainerFormula() {
        assertEquals(scale(5.0F / 9.0F), RedstoneMode.fullness(stacks(9, 5)));
        assertEquals(scale(2.0F / 9.0F), RedstoneMode.fullness(stacks(9, 2)));
    }

    @Test
    void zeroSlotHandlerReadsZeroInsteadOfDividingByZero() {
        assertEquals(0, RedstoneMode.fullness(new ItemStacksResourceHandler(0)));
    }

    @Test
    void unstackableItemFillsItsSlotCompletely() {
        ItemStacksResourceHandler handler = new ItemStacksResourceHandler(1);
        handler.set(0, ItemResource.of(Items.DIAMOND_PICKAXE), 1);
        assertEquals(15, RedstoneMode.fullness(handler));
    }

    private static int scale(float fill) {
        return (int) Math.floor(fill * 14.0F) + (fill > 0.0F ? 1 : 0);
    }

    private static ItemStacksResourceHandler stacks(int slots, int fullSlots) {
        ItemStacksResourceHandler handler = new ItemStacksResourceHandler(slots);
        for (int slot = 0; slot < fullSlots; slot++) {
            handler.set(slot, ItemResource.of(Items.COBBLESTONE), 64);
        }
        return handler;
    }
}
