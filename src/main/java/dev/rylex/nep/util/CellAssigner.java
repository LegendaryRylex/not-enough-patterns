package dev.rylex.nep.util;

import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class CellAssigner {
    private CellAssigner() {}

    public static int @Nullable [] assign(long[] keyCapacities, List<int[]> cellCompatibleKeys) {
        int cellCount = cellCompatibleKeys.size();
        long total = 0;
        for (long capacity : keyCapacities) {
            if (capacity < 0) {
                return null;
            }
            total += capacity;
        }
        if (total != cellCount) {
            return null;
        }

        int[] slotKey = new int[cellCount];
        int[][] keySlots = new int[keyCapacities.length][];
        int slot = 0;
        for (int key = 0; key < keyCapacities.length; key++) {
            keySlots[key] = new int[(int) keyCapacities[key]];
            for (int i = 0; i < keyCapacities[key]; i++) {
                keySlots[key][i] = slot;
                slotKey[slot] = key;
                slot++;
            }
        }

        int[] slotToCell = new int[cellCount];
        int[] cellToSlot = new int[cellCount];
        Arrays.fill(slotToCell, -1);
        Arrays.fill(cellToSlot, -1);

        for (int cell = 0; cell < cellCount; cell++) {
            if (!augment(cell, cellCompatibleKeys, keySlots, slotToCell, cellToSlot, new boolean[cellCount])) {
                return null;
            }
        }

        int[] assignment = new int[cellCount];
        for (int cell = 0; cell < cellCount; cell++) {
            assignment[cell] = slotKey[cellToSlot[cell]];
        }
        return assignment;
    }

    private static boolean augment(
            int cell,
            List<int[]> cellCompatibleKeys,
            int[][] keySlots,
            int[] slotToCell,
            int[] cellToSlot,
            boolean[] visited) {
        for (int key : cellCompatibleKeys.get(cell)) {
            for (int slot : keySlots[key]) {
                if (visited[slot]) {
                    continue;
                }
                visited[slot] = true;
                if (slotToCell[slot] == -1
                        || augment(slotToCell[slot], cellCompatibleKeys, keySlots, slotToCell, cellToSlot, visited)) {
                    slotToCell[slot] = cell;
                    cellToSlot[cell] = slot;
                    return true;
                }
            }
        }
        return false;
    }
}
