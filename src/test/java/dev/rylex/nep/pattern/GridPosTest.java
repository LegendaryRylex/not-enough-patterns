package dev.rylex.nep.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class GridPosTest {

    @Test
    void boundsOfEmptyCollectionIsNull() {
        assertNull(GridPos.bounds(List.of()));
    }

    @Test
    void boundsSpanAllPositions() {
        var bounds = GridPos.bounds(List.of(new GridPos(-1, 2), new GridPos(3, 0), new GridPos(0, 5)));
        assertEquals(new GridPos.Bounds(-1, 0, 3, 5), bounds);
        assertEquals(5, bounds.width());
        assertEquals(6, bounds.height());
    }

    @Test
    void topLeftCellIsIndexZero() {
        var bounds = GridPos.bounds(List.of(new GridPos(0, 0), new GridPos(2, 2)));
        assertEquals(0, bounds.rowMajorIndex(new GridPos(0, 2)));
    }

    @Test
    void gridYGrowsUpwardWhileRowsGrowDownward() {
        var bounds = new GridPos.Bounds(0, 0, 2, 1);
        assertEquals(0, bounds.rowMajorIndex(new GridPos(0, 1)));
        assertEquals(1, bounds.rowMajorIndex(new GridPos(1, 1)));
        assertEquals(2, bounds.rowMajorIndex(new GridPos(2, 1)));
        assertEquals(3, bounds.rowMajorIndex(new GridPos(0, 0)));
        assertEquals(5, bounds.rowMajorIndex(new GridPos(2, 0)));
    }

    @Test
    void indexIsRelativeToBoundsOrigin() {
        var bounds = new GridPos.Bounds(-2, -3, 0, -1);
        assertEquals(0, bounds.rowMajorIndex(new GridPos(-2, -1)));
        assertEquals(8, bounds.rowMajorIndex(new GridPos(0, -3)));
    }

    @Test
    void offsetMoves() {
        assertEquals(new GridPos(1, -1), new GridPos(0, 0).offset(1, -1));
    }
}
