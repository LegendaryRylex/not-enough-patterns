package dev.rylex.nep.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OwedOutputTest {

    private static final long NOW = 1_000L;
    private static final long GRACE = 200L;

    @Test
    void aFreshDebtCarriesNoDeadline() {
        OwedOutput owed = new OwedOutput(4);

        assertFalse(owed.hasDeadline());
        assertFalse(owed.expired(Long.MAX_VALUE));
    }

    @Test
    void aLiveJobNeverExpiresHoweverLongTheMachineTakes() {
        OwedOutput owed = new OwedOutput(4);
        owed.startGrace(NOW + GRACE);
        owed.clearGrace();

        assertFalse(owed.hasDeadline());
        assertFalse(owed.expired(NOW + GRACE * 1_000));
    }

    @Test
    void theGraceRunsOutOnlyOnceTheDeadlineIsReached() {
        OwedOutput owed = new OwedOutput(4);
        owed.startGrace(NOW + GRACE);

        assertFalse(owed.expired(NOW));
        assertFalse(owed.expired(NOW + GRACE - 1));
        assertTrue(owed.expired(NOW + GRACE));
        assertTrue(owed.expired(NOW + GRACE + 1));
    }

    @Test
    void aZeroGraceIsUpAtTheVeryNextCheck() {
        OwedOutput owed = new OwedOutput(4);
        owed.startGrace(NOW);

        assertTrue(owed.expired(NOW));
    }

    @Test
    void everyCollectionPushesTheDeadlineOutAgain() {
        OwedOutput owed = new OwedOutput(4);
        owed.startGrace(NOW + GRACE);
        assertFalse(owed.take(1));

        owed.startGrace(NOW + 50 + GRACE);

        assertFalse(owed.expired(NOW + GRACE));
        assertTrue(owed.expired(NOW + 50 + GRACE));
    }

    @Test
    void newWorkOnTheSameEntryCallsOffTheGrace() {
        OwedOutput owed = new OwedOutput(4);
        owed.startGrace(NOW + GRACE);

        owed.add(2);

        assertEquals(6, owed.amount());
        assertFalse(owed.hasDeadline());
        assertFalse(owed.expired(NOW + GRACE * 1_000));
    }

    @Test
    void takingTheLastOfTheDebtReportsItSettled() {
        OwedOutput owed = new OwedOutput(3);

        assertFalse(owed.take(1));
        assertEquals(2, owed.amount());
        assertTrue(owed.take(2));
    }

    @Test
    void anEntryFromAnOlderBuildLoadsWithoutADeadlineAndStillTakesOne() {
        OwedOutput owed = new OwedOutput(2, OwedOutput.NO_DEADLINE);

        assertFalse(owed.hasDeadline());
        assertFalse(owed.expired(Long.MAX_VALUE));

        owed.startGrace(NOW + GRACE);

        assertTrue(owed.expired(NOW + GRACE));
    }
}
