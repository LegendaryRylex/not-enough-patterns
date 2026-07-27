package dev.rylex.nep.compat.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MatrixStressCurveTest {

    private static final int MIN_SPEED = 32;
    private static final float PEAK_SPEED = 256.0F;
    private static final int MIN_STRESS = 8_192;
    private static final int MAX_STRESS = 294_912;

    private static int stress(float rpm) {
        return MatrixStressCurve.stressAt(rpm, MIN_SPEED, PEAK_SPEED, MIN_STRESS, MAX_STRESS);
    }

    @Test
    void belowMinimumSpeedDrawsNothing() {
        assertEquals(0, stress(0));
        assertEquals(0, stress(31.9F));
    }

    @Test
    void rampSpansTheConfiguredStressBand() {
        assertEquals(MIN_STRESS, stress(MIN_SPEED));
        assertEquals(MAX_STRESS, stress(PEAK_SPEED));
    }

    @Test
    void reverseRotationMatchesForward() {
        assertEquals(stress(128), stress(-128));
    }

    @Test
    void overspeedNeverExceedsTheCeiling() {
        assertEquals(MAX_STRESS, stress(PEAK_SPEED * 10));
    }

    @Test
    void drawRisesMonotonicallyAndStaysInsideTheBand() {
        int previous = 0;
        for (float rpm = 0; rpm <= PEAK_SPEED * 2; rpm += 0.5F) {
            int current = stress(rpm);
            assertTrue(current >= previous, "draw fell at " + rpm + " RPM: " + previous + " -> " + current);
            assertTrue(current == 0 || current >= MIN_STRESS, "draw below the floor at " + rpm + " RPM: " + current);
            assertTrue(current <= MAX_STRESS, "draw above the ceiling at " + rpm + " RPM: " + current);
            previous = current;
        }
    }

    @Test
    void minimumSpeedAboveCreatesPeakIsClampedSoTheMatrixStaysUsable() {
        assertEquals(PEAK_SPEED, MatrixStressCurve.effectiveMinimumSpeed(1_024, PEAK_SPEED));
        assertEquals(MAX_STRESS, MatrixStressCurve.stressAt(PEAK_SPEED, 1_024, PEAK_SPEED, MIN_STRESS, MAX_STRESS));
        assertEquals(0, MatrixStressCurve.stressAt(PEAK_SPEED - 1, 1_024, PEAK_SPEED, MIN_STRESS, MAX_STRESS));
    }

    @Test
    void minimumSpeedEqualToPeakDrawsFullStressWithoutDividingByZero() {
        assertEquals(MAX_STRESS, MatrixStressCurve.stressAt(256, 256, PEAK_SPEED, MIN_STRESS, MAX_STRESS));
    }

    @Test
    void invertedStressBandNeverGoesNegativeOrOverTheCeiling() {
        for (float rpm = 0; rpm <= PEAK_SPEED; rpm += 8) {
            int drawn = MatrixStressCurve.stressAt(rpm, MIN_SPEED, PEAK_SPEED, MAX_STRESS, MIN_STRESS);
            assertTrue(drawn >= 0, "negative draw at " + rpm + " RPM: " + drawn);
            assertTrue(drawn <= MIN_STRESS, "draw above the ceiling at " + rpm + " RPM: " + drawn);
        }
    }

    @Test
    void nonPositivePeakSpeedNeverDividesByZero() {
        assertEquals(0, MatrixStressCurve.stressAt(0, MIN_SPEED, 0, MIN_STRESS, MAX_STRESS));
        assertEquals(MAX_STRESS, MatrixStressCurve.stressAt(64, MIN_SPEED, 0, MIN_STRESS, MAX_STRESS));
    }

    @Test
    void configuredExtremesStayWithinLongRange() {
        long work = MatrixStressCurve.workPerCraft(1_200, 16_777_216);
        assertTrue(work > 0, "work per craft overflowed: " + work);
        assertEquals(1_200L * 16_777_216L, work);
        assertTrue(work < Long.MAX_VALUE / 2);
    }

    @Test
    void workPerCraftIsNeverZeroSoProgressAlwaysTerminates() {
        assertEquals(1L, MatrixStressCurve.workPerCraft(1, 1));
        assertTrue(MatrixStressCurve.workPerCraft(0, 0) >= 1);
    }

    @Test
    void everyConfigCornerProducesASaneDraw() {
        int[] minimumSpeeds = {1, 32, 256, 1_024};
        int[] minimumStresses = {1, 8_192, 16_777_216};
        int[] maximumStresses = {1, 294_912, 16_777_216};
        for (int minimumSpeed : minimumSpeeds) {
            for (int minimumStress : minimumStresses) {
                for (int maximumStress : maximumStresses) {
                    for (float rpm = 0; rpm <= PEAK_SPEED; rpm += 16) {
                        int drawn =
                                MatrixStressCurve.stressAt(rpm, minimumSpeed, PEAK_SPEED, minimumStress, maximumStress);
                        String at = "minSpeed=" + minimumSpeed + " minStress=" + minimumStress + " maxStress="
                                + maximumStress + " rpm=" + rpm;
                        assertTrue(drawn >= 0, "negative draw: " + at);
                        assertTrue(drawn <= maximumStress, "draw above the ceiling: " + at);
                    }
                }
            }
        }
    }
}
