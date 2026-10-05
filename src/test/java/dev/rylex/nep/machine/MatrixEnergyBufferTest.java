package dev.rylex.nep.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MatrixEnergyBufferTest {

    @Test
    void aPartialTailFillsTheBufferToItsExactCapacity() {
        MatrixEnergyBuffer buffer = new MatrixEnergyBuffer(999L, 100L);
        for (int tick = 0; tick < 20; tick++) {
            buffer.receive(Long.MAX_VALUE, false);
        }
        assertEquals(999L, buffer.stored());
    }

    @Test
    void anEmptyBufferIsTheBottomLevelAndAFullOneIsTheTop() {
        MatrixEnergyBuffer buffer = new MatrixEnergyBuffer(Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(0, buffer.syncLevel());
        buffer.receive(Integer.MAX_VALUE, false);
        assertEquals(MatrixEnergyBuffer.SYNC_STEPS, buffer.syncLevel());
    }

    @Test
    void aCapacityThatIsNoMultipleOfTheStepCountStillReachesTheTopLevelOnlyWhenFull() {
        MatrixEnergyBuffer buffer = new MatrixEnergyBuffer(Integer.MAX_VALUE, 100_000L);
        long tail = 0L;
        while (buffer.stored() < Integer.MAX_VALUE) {
            tail = buffer.receive(Long.MAX_VALUE, false);
            if (buffer.stored() < Integer.MAX_VALUE) {
                assertNotEquals(MatrixEnergyBuffer.SYNC_STEPS, buffer.syncLevel());
            }
        }
        assertTrue(tail < 100_000L);
        assertEquals(MatrixEnergyBuffer.SYNC_STEPS, buffer.syncLevel());
    }

    @Test
    void theLevelTracksTheStoredEnergyWithinOneStep() {
        long capacity = Integer.MAX_VALUE;
        MatrixEnergyBuffer buffer = new MatrixEnergyBuffer(capacity, capacity);
        for (int level = 1; level < MatrixEnergyBuffer.SYNC_STEPS; level++) {
            MatrixEnergyBuffer probe = new MatrixEnergyBuffer(capacity, capacity);
            probe.receive(capacity * level / MatrixEnergyBuffer.SYNC_STEPS, false);
            assertEquals(level, probe.syncLevel());
        }
        assertEquals(0L, buffer.stored());
    }

    @Test
    void aCapacityBelowTheStepCountReportsExactly() {
        MatrixEnergyBuffer buffer = new MatrixEnergyBuffer(7L, 1L);
        for (int expected = 1; expected <= 6; expected++) {
            buffer.receive(1L, false);
            assertEquals(expected, buffer.syncLevel());
        }
        buffer.receive(1L, false);
        assertEquals(MatrixEnergyBuffer.SYNC_STEPS, buffer.syncLevel());
    }
}
