package dev.rylex.nep.pattern;

import appeng.api.stacks.AEItemKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public record GridPlan(int width, int height, List<@Nullable AEItemKey> cells) {

    public GridPlan {
        cells = Collections.unmodifiableList(cells);
    }

    public List<Integer> filledIndices() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) {
            if (cells.get(i) != null) {
                result.add(i);
            }
        }
        return result;
    }

    public Map<AEItemKey, Long> multiset() {
        Map<AEItemKey, Long> result = new HashMap<>();
        for (AEItemKey cell : cells) {
            if (cell != null) {
                result.merge(cell, 1L, Long::sum);
            }
        }
        return result;
    }
}
