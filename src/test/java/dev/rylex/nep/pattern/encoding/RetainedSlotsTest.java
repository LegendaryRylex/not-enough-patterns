package dev.rylex.nep.pattern.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class RetainedSlotsTest {

    private static final AEItemKey IRON = AEItemKey.of(Items.IRON_INGOT);
    private static final AEItemKey GOLD = AEItemKey.of(Items.GOLD_INGOT);

    private static GenericStack stack(AEItemKey key, long amount) {
        return new GenericStack(key, amount);
    }

    private static List<GenericStack> slots(GenericStack... stacks) {
        return Arrays.asList(stacks);
    }

    @Test
    void theSlotHoldingTheKeptAmountIsMarked() {
        assertEquals(List.of(1), RetainedSlots.slotsOf(slots(stack(IRON, 2), stack(GOLD, 1)), List.of(stack(GOLD, 1))));
    }

    @Test
    void anItemThatIsBothConsumedAndKeptMarksOnlyTheKeptSlot() {
        assertEquals(List.of(1), RetainedSlots.slotsOf(slots(stack(IRON, 2), stack(IRON, 1)), List.of(stack(IRON, 1))));
    }

    @Test
    void twoIdenticalSlotsMarkOnlyOne() {
        assertEquals(List.of(1), RetainedSlots.slotsOf(slots(stack(IRON, 1), stack(IRON, 1)), List.of(stack(IRON, 1))));
    }

    @Test
    void aKeptAmountSpreadOverSlotsMarksAllOfThem() {
        assertEquals(
                List.of(1, 2),
                RetainedSlots.slotsOf(slots(stack(GOLD, 4), stack(IRON, 1), stack(IRON, 1)), List.of(stack(IRON, 2))));
    }

    @Test
    void emptySlotsAreSkipped() {
        assertEquals(List.of(1), RetainedSlots.slotsOf(slots(null, stack(GOLD, 1)), List.of(stack(GOLD, 1))));
    }

    @Test
    void aKeptAmountNoSlotCanCoverMarksNothing() {
        assertEquals(List.of(), RetainedSlots.slotsOf(slots(stack(IRON, 3)), List.of(stack(IRON, 2))));
    }

    @Test
    void severalKeptKeysAreMarkedInSlotOrder() {
        assertEquals(
                List.of(0, 2),
                RetainedSlots.slotsOf(
                        slots(stack(GOLD, 1), stack(IRON, 4), stack(IRON, 1)),
                        List.of(stack(IRON, 1), stack(GOLD, 1))));
    }
}
