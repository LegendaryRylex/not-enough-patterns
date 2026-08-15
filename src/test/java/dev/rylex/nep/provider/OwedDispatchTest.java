package dev.rylex.nep.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.stacks.AEItemKey;
import java.util.Map;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class OwedDispatchTest {

    private static final AEItemKey IRON = AEItemKey.of(Items.IRON_INGOT);
    private static final AEItemKey GOLD = AEItemKey.of(Items.GOLD_INGOT);
    private static final AEItemKey DIAMOND = AEItemKey.of(Items.DIAMOND);

    @Test
    void anInputThatIsNeverHandedBackIsReserved() {
        OwedDispatch dispatch = new OwedDispatch(Map.of(IRON, 2L), Map.of(GOLD, 1L));

        assertEquals(2, dispatch.reserved(IRON));
        assertEquals(0, dispatch.reserved(GOLD));
        assertEquals(1, dispatch.owedAmount(GOLD));
    }

    @Test
    void anInputTheDispatchOwesBackIsNotReserved() {
        OwedDispatch dispatch = new OwedDispatch(Map.of(GOLD, 1L, DIAMOND, 1L), Map.of(GOLD, 1L));

        assertEquals(0, dispatch.reserved(GOLD));
        assertEquals(1, dispatch.reserved(DIAMOND));
    }

    @Test
    void aPartiallyOwedInputReservesOnlyTheSurplus() {
        OwedDispatch dispatch = new OwedDispatch(Map.of(GOLD, 3L), Map.of(GOLD, 1L));

        assertEquals(2, dispatch.reserved(GOLD));
    }

    @Test
    void takingTheWholeOwedAmountSettlesTheDispatch() {
        OwedDispatch dispatch = new OwedDispatch(Map.of(IRON, 1L), Map.of(GOLD, 2L));

        assertEquals(1, dispatch.take(GOLD, 1));
        assertFalse(dispatch.isSettled());
        assertEquals(1, dispatch.take(GOLD, 1));
        assertTrue(dispatch.isSettled());
    }

    @Test
    void takingNeverExceedsWhatIsOwed() {
        OwedDispatch dispatch = new OwedDispatch(Map.of(), Map.of(GOLD, 1L));

        assertEquals(1, dispatch.take(GOLD, 8));
        assertEquals(0, dispatch.take(GOLD, 8));
        assertEquals(0, dispatch.take(DIAMOND, 8));
    }

    @Test
    void progressRestartsTheGraceCountdown() {
        OwedDispatch dispatch = new OwedDispatch(Map.of(), Map.of(GOLD, 2L));
        dispatch.startGrace(100);
        assertTrue(dispatch.hasDeadline());

        dispatch.take(GOLD, 1);

        assertFalse(dispatch.hasDeadline());
        assertFalse(dispatch.expired(1000));
    }

    @Test
    void aDispatchExpiresOnlyOnceItsDeadlinePasses() {
        OwedDispatch dispatch = new OwedDispatch(Map.of(), Map.of(GOLD, 1L));

        assertFalse(dispatch.expired(100));
        dispatch.startGrace(100);
        assertFalse(dispatch.expired(99));
        assertTrue(dispatch.expired(100));

        dispatch.clearGrace();
        assertFalse(dispatch.expired(1000));
    }
}
