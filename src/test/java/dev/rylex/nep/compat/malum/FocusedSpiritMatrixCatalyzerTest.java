package dev.rylex.nep.compat.malum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class FocusedSpiritMatrixCatalyzerTest {

    private static final int FULL = FocusedSpiritMatrixUpgrades.maxCatalyzers();

    private static final int TRIALS = 20_000;

    private static double meanRolls(double chance) {
        RandomSource random = RandomSource.create(1234L);
        long total = 0;
        for (int trial = 0; trial < TRIALS; trial++) {
            total += FocusedSpiritMatrixUpgrades.rollChance(chance, random);
        }
        return total / (double) TRIALS;
    }

    @Test
    void anEmptySlotGrantsNothing() {
        assertEquals(0.0, FocusedSpiritMatrixUpgrades.restorationChance(0));
        assertEquals(0.0, FocusedSpiritMatrixUpgrades.chainFocusingChance(0));
        assertEquals(0.0, FocusedSpiritMatrixUpgrades.fortuneChance(0));
    }

    @Test
    void aFullSlotReachesTheConfiguredCaps() {
        assertEquals(0.50, FocusedSpiritMatrixUpgrades.restorationChance(FULL), 1.0e-6);
        assertEquals(0.25, FocusedSpiritMatrixUpgrades.chainFocusingChance(FULL), 1.0e-6);
        assertEquals(0.25, FocusedSpiritMatrixUpgrades.fortuneChance(FULL), 1.0e-6);
    }

    @Test
    void oneCatalyzerIsWorthItsCapOverTheSlotSize() {
        assertEquals(
                FocusedSpiritMatrixUpgrades.restorationChance(FULL) / FULL,
                FocusedSpiritMatrixUpgrades.restorationChance(1),
                1.0e-6);
        assertEquals(
                FocusedSpiritMatrixUpgrades.fortuneChance(FULL) / FULL,
                FocusedSpiritMatrixUpgrades.fortuneChance(1),
                1.0e-6);
    }

    @Test
    void aPartlyFilledSlotScalesLinearly() {
        assertEquals(
                FocusedSpiritMatrixUpgrades.restorationChance(FULL) / 2.0,
                FocusedSpiritMatrixUpgrades.restorationChance(FULL / 2),
                1.0e-6);
        double previous = -1.0;
        for (int catalyzers = 0; catalyzers <= FULL; catalyzers++) {
            double chance = FocusedSpiritMatrixUpgrades.fortuneChance(catalyzers);
            assertTrue(chance > previous, "fortune did not rise with catalyzer " + catalyzers);
            previous = chance;
        }
    }

    @Test
    void anOverfilledSlotIsCappedAtAFullOne() {
        assertEquals(
                FocusedSpiritMatrixUpgrades.fortuneChance(FULL),
                FocusedSpiritMatrixUpgrades.fortuneChance(FULL * 4),
                1.0e-6);
    }

    @Test
    void noChanceNeverSucceeds() {
        RandomSource random = RandomSource.create(1234L);
        for (int trial = 0; trial < TRIALS; trial++) {
            assertEquals(0, FocusedSpiritMatrixUpgrades.rollChance(0.0, random));
        }
    }

    @Test
    void aWholeChanceAlwaysSucceedsWhateverTheRoll() {
        RandomSource random = RandomSource.create(1234L);
        for (int trial = 0; trial < TRIALS; trial++) {
            assertEquals(1, FocusedSpiritMatrixUpgrades.rollChance(1.0, random));
            assertEquals(2, FocusedSpiritMatrixUpgrades.rollChance(2.0, random));
        }
    }

    @Test
    void aFractionalChanceIsOneRollAtMost() {
        RandomSource random = RandomSource.create(1234L);
        for (int trial = 0; trial < TRIALS; trial++) {
            int rolls = FocusedSpiritMatrixUpgrades.rollChance(0.25, random);
            assertTrue(rolls == 0 || rolls == 1, "a quarter chance yielded " + rolls + " successes");
        }
        assertEquals(0.25, meanRolls(0.25), 0.02);
    }

    @Test
    void aChanceOverOneGuaranteesTheWholePartAndRollsTheRest() {
        RandomSource random = RandomSource.create(1234L);
        for (int trial = 0; trial < TRIALS; trial++) {
            int rolls = FocusedSpiritMatrixUpgrades.rollChance(2.5, random);
            assertTrue(rolls == 2 || rolls == 3, "a 250% chance yielded " + rolls + " successes");
        }
        assertEquals(2.5, meanRolls(2.5), 0.02);
    }
}
