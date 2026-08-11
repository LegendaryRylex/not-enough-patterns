package dev.rylex.nep.pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GridPlacementTest {

    private static final List<Integer> FULL_3X3 = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8);

    private static Set<GridPos> rectangle(int width, int height) {
        Set<GridPos> positions = new HashSet<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                positions.add(new GridPos(x, y));
            }
        }
        return positions;
    }

    @Test
    void equalDistanceTiesResolveToTheFirstWindowInScanOrder() {
        Set<GridPos> open = new HashSet<>(Set.of(new GridPos(0, 0), new GridPos(2, 0)));
        var placement = GridPlacement.find(1, List.of(0), open, new GridPos(1, 0));
        assertArrayEquals(
                new GridPos[] {new GridPos(0, 0)},
                placement,
                "both spots are one step from the target; the left one is scanned first and must win");
    }

    @Test
    void exactFitPlacesEveryCell() {
        var placement = GridPlacement.find(2, List.of(0, 1, 2, 3), rectangle(2, 2), new GridPos(0, 0));
        assertArrayEquals(
                new GridPos[] {new GridPos(0, 1), new GridPos(1, 1), new GridPos(0, 0), new GridPos(1, 0)}, placement);
    }

    @Test
    void snapsToBottomLeftTarget() {
        var placement = GridPlacement.find(3, FULL_3X3, rectangle(5, 5), new GridPos(0, 0));
        assertArrayEquals(
                new GridPos[] {
                    new GridPos(0, 2), new GridPos(1, 2), new GridPos(2, 2),
                    new GridPos(0, 1), new GridPos(1, 1), new GridPos(2, 1),
                    new GridPos(0, 0), new GridPos(1, 0), new GridPos(2, 0)
                },
                placement);
    }

    @Test
    void snapsToTopRightTarget() {
        var placement = GridPlacement.find(3, FULL_3X3, rectangle(5, 5), new GridPos(4, 4));
        assertArrayEquals(
                new GridPos[] {
                    new GridPos(2, 4), new GridPos(3, 4), new GridPos(4, 4),
                    new GridPos(2, 3), new GridPos(3, 3), new GridPos(4, 3),
                    new GridPos(2, 2), new GridPos(3, 2), new GridPos(4, 2)
                },
                placement);
    }

    @Test
    void snapsToCentralTarget() {
        var placement = GridPlacement.find(3, FULL_3X3, rectangle(5, 5), new GridPos(2, 2));
        assertArrayEquals(
                new GridPos[] {
                    new GridPos(1, 3), new GridPos(2, 3), new GridPos(3, 3),
                    new GridPos(1, 2), new GridPos(2, 2), new GridPos(3, 2),
                    new GridPos(1, 1), new GridPos(2, 1), new GridPos(3, 1)
                },
                placement);
    }

    @Test
    void emptyPlanColumnsMayOverhangTheGrid() {
        var placement = GridPlacement.find(3, List.of(2, 5, 8), rectangle(1, 3), new GridPos(0, 0));
        assertArrayEquals(new GridPos[] {new GridPos(0, 2), new GridPos(0, 1), new GridPos(0, 0)}, placement);
    }

    @Test
    void blockedPositionsRerouteToNextClosestSpot() {
        Set<GridPos> open = rectangle(5, 5);
        open.remove(new GridPos(1, 0));
        var placement = GridPlacement.find(3, FULL_3X3, open, new GridPos(0, 0));
        assertArrayEquals(
                new GridPos[] {
                    new GridPos(0, 3), new GridPos(1, 3), new GridPos(2, 3),
                    new GridPos(0, 2), new GridPos(1, 2), new GridPos(2, 2),
                    new GridPos(0, 1), new GridPos(1, 1), new GridPos(2, 1)
                },
                placement);
    }

    @Test
    void noPlacementWhenAHoleBlocksEveryWindow() {
        Set<GridPos> open = rectangle(5, 5);
        open.remove(new GridPos(2, 2));
        assertNull(GridPlacement.find(3, FULL_3X3, open, new GridPos(0, 0)));
    }

    @Test
    void nonRectangularGridsWork() {
        Set<GridPos> open = Set.of(new GridPos(0, 1), new GridPos(0, 0), new GridPos(1, 0));
        var placement = GridPlacement.find(2, List.of(0, 2, 3), open, new GridPos(0, 0));
        assertArrayEquals(new GridPos[] {new GridPos(0, 1), new GridPos(0, 0), new GridPos(1, 0)}, placement);
    }

    @Test
    void planLargerThanGridFails() {
        assertNull(GridPlacement.find(3, FULL_3X3, rectangle(2, 2), new GridPos(0, 0)));
    }

    @Test
    void emptyInputsFail() {
        assertNull(GridPlacement.find(3, List.of(), rectangle(3, 3), new GridPos(0, 0)));
        assertNull(GridPlacement.find(3, List.of(0), Set.of(), new GridPos(0, 0)));
    }
}
