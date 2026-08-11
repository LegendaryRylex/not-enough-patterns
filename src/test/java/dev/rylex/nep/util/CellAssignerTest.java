package dev.rylex.nep.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class CellAssignerTest {

    @Test
    void assignsUniqueCompatibilities() {
        int[] assignment = CellAssigner.assign(new long[] {1, 1}, List.of(new int[] {0}, new int[] {1}));
        assertArrayEquals(new int[] {0, 1}, assignment);
    }

    @Test
    void spreadsCapacityAcrossCells() {
        int[] assignment = CellAssigner.assign(new long[] {3}, List.of(new int[] {0}, new int[] {0}, new int[] {0}));
        assertArrayEquals(new int[] {0, 0, 0}, assignment);
    }

    @Test
    void backtracksWhenGreedyChoiceBlocksAnotherCell() {
        int[] assignment = CellAssigner.assign(new long[] {1, 1}, List.of(new int[] {0, 1}, new int[] {0}));
        assertArrayEquals(new int[] {1, 0}, assignment);
    }

    @Test
    void backtracksThroughChains() {
        int[] assignment =
                CellAssigner.assign(new long[] {1, 1, 1}, List.of(new int[] {0, 1}, new int[] {1, 2}, new int[] {1}));
        assertNotNull(assignment);
        assertArrayEquals(new int[] {0, 2, 1}, assignment);
    }

    @Test
    void nullWhenCapacityTotalMismatchesCellCount() {
        assertNull(CellAssigner.assign(new long[] {2}, List.of(new int[] {0})));
        assertNull(CellAssigner.assign(new long[] {1}, List.of(new int[] {0}, new int[] {0})));
    }

    @Test
    void nullWhenNoPerfectMatchingExists() {
        assertNull(CellAssigner.assign(new long[] {2, 0}, List.of(new int[] {0}, new int[] {1})));
    }

    @Test
    void nullOnNegativeCapacity() {
        assertNull(CellAssigner.assign(new long[] {-1, 2}, List.of(new int[] {0})));
    }

    @Test
    void handlesLargeGrid() {
        long[] capacities = new long[] {40, 41};
        int[][] compat = new int[81][];
        for (int i = 0; i < 81; i++) {
            compat[i] = new int[] {0, 1};
        }
        int[] assignment = CellAssigner.assign(capacities, List.of(compat));
        assertNotNull(assignment);
        int zeros = 0;
        for (int key : assignment) {
            if (key == 0) {
                zeros++;
            }
        }
        assertArrayEquals(new int[] {40, 41}, new int[] {zeros, 81 - zeros});
    }
}
