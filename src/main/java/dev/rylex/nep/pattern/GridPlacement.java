package dev.rylex.nep.pattern;

import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public final class GridPlacement {
    private GridPlacement() {}

    @Nullable
    public static GridPos[] find(int planWidth, List<Integer> filledCells, Set<GridPos> openPositions, GridPos target) {
        if (filledCells.isEmpty()) {
            return null;
        }
        GridPos.Bounds bounds = GridPos.bounds(openPositions);
        if (bounds == null) {
            return null;
        }

        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        for (int cell : filledCells) {
            int col = cell % planWidth;
            int row = cell / planWidth;
            minCol = Math.min(minCol, col);
            maxCol = Math.max(maxCol, col);
            minRow = Math.min(minRow, row);
            maxRow = Math.max(maxRow, row);
        }

        GridPos[] best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int ty = bounds.maxY() + minRow; ty >= bounds.minY() + maxRow; ty--) {
            for (int tx = bounds.minX() - minCol; tx <= bounds.maxX() - maxCol; tx++) {
                GridPos[] placement = tryAt(planWidth, filledCells, openPositions, tx, ty);
                if (placement == null) {
                    continue;
                }
                int distance = 0;
                for (GridPos pos : placement) {
                    distance += Math.abs(pos.x() - target.x()) + Math.abs(pos.y() - target.y());
                }
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = placement;
                }
            }
        }
        return best;
    }

    @Nullable
    private static GridPos[] tryAt(
            int planWidth, List<Integer> filledCells, Set<GridPos> openPositions, int tx, int ty) {
        GridPos[] placement = new GridPos[filledCells.size()];
        for (int i = 0; i < filledCells.size(); i++) {
            int cell = filledCells.get(i);
            GridPos pos = new GridPos(tx + cell % planWidth, ty - cell / planWidth);
            if (!openPositions.contains(pos)) {
                return null;
            }
            placement[i] = pos;
        }
        return placement;
    }
}
