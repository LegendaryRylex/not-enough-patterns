package dev.rylex.nep.compat.create;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class StationLayouts {
    private StationLayouts() {}

    static List<StationKind> unknown(int size) {
        return new ArrayList<>(Collections.nCopies(size, StationKind.UNKNOWN));
    }

    static boolean anyKnown(List<StationKind> kinds) {
        for (StationKind kind : kinds) {
            if (kind.recognized()) {
                return true;
            }
        }
        return false;
    }

    static List<StationKind> expected(List<List<StationKind>> layouts, List<StationKind> observed) {
        List<StationKind> hints = unknown(observed.size());
        List<List<StationKind>> fitting = new ArrayList<>();
        for (List<StationKind> layout : layouts) {
            if (fits(layout, observed)) {
                fitting.add(layout);
            }
        }
        if (fitting.isEmpty()) {
            return hints;
        }
        for (int slot = 0; slot < observed.size(); slot++) {
            if (observed.get(slot).recognized()) {
                continue;
            }
            StationKind agreed = fitting.get(0).get(slot);
            for (List<StationKind> layout : fitting) {
                if (layout.get(slot) != agreed) {
                    agreed = StationKind.UNKNOWN;
                    break;
                }
            }
            hints.set(slot, agreed);
        }
        return hints;
    }

    private static boolean fits(List<StationKind> layout, List<StationKind> observed) {
        if (layout.size() != observed.size()) {
            return false;
        }
        for (int slot = 0; slot < observed.size(); slot++) {
            StationKind seen = observed.get(slot);
            if (seen.recognized() && layout.get(slot) != seen) {
                return false;
            }
        }
        return true;
    }
}
