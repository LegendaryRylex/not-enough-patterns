package dev.rylex.nep.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

class RedstoneModeTest {

    private static final int TANK_CAPACITY = 64_000;
    private static final int TANKS = 4;
    private static final int INPUT_SLOTS = 18;

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
        assertEquals(0, RedstoneMode.fullness(new ItemStackHandler(9)));

        ItemStackHandler one = new ItemStackHandler(9);
        one.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 1));
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
        assertEquals(0, RedstoneMode.fullness(new ItemStackHandler(0)));
    }

    @Test
    void unstackableItemFillsItsSlotCompletely() {
        ItemStackHandler handler = new ItemStackHandler(1);
        handler.setStackInSlot(0, new ItemStack(Items.DIAMOND_PICKAXE));
        assertEquals(15, RedstoneMode.fullness(handler));
    }

    @Test
    void inputFullnessAveragesItemSlotsAndFluidTanksAsOneCellEach() {
        assertEquals(
                scale((float) INPUT_SLOTS / (INPUT_SLOTS + TANKS)),
                RedstoneMode.inputFullness(stacks(INPUT_SLOTS, INPUT_SLOTS), tanks(0, 0, 0, 0)));
    }

    @Test
    void fluidsAloneCarryOnlyFourOfTwentyTwoCells() {
        int signal = RedstoneMode.inputFullness(
                new ItemStackHandler(INPUT_SLOTS), tanks(TANK_CAPACITY, TANK_CAPACITY, TANK_CAPACITY, TANK_CAPACITY));

        assertEquals(scale((float) TANKS / (INPUT_SLOTS + TANKS)), signal);
        assertEquals(3, signal);
    }

    @Test
    void halfFullTankCountsAsHalfACell() {
        assertEquals(
                scale(0.5F / (INPUT_SLOTS + TANKS)),
                RedstoneMode.inputFullness(new ItemStackHandler(INPUT_SLOTS), tanks(TANK_CAPACITY / 2, 0, 0, 0)));
    }

    @Test
    void aCompletelyFullInputReadsFifteen() {
        assertEquals(
                15,
                RedstoneMode.inputFullness(
                        stacks(INPUT_SLOTS, INPUT_SLOTS),
                        tanks(TANK_CAPACITY, TANK_CAPACITY, TANK_CAPACITY, TANK_CAPACITY)));
    }

    @Test
    void emptyInputReadsZero() {
        assertEquals(0, RedstoneMode.inputFullness(new ItemStackHandler(INPUT_SLOTS), tanks(0, 0, 0, 0)));
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

    private static IFluidHandler tanks(int... amounts) {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return amounts.length;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return amounts[tank] <= 0 ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, amounts[tank]);
            }

            @Override
            public int getTankCapacity(int tank) {
                return TANK_CAPACITY;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return true;
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return 0;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return FluidStack.EMPTY;
            }
        };
    }
}
