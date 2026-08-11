package dev.rylex.nep.pattern;

import java.util.Collection;
import org.jetbrains.annotations.Nullable;

public record GridPos(int x, int y) {

    public GridPos offset(int dx, int dy) {
        return new GridPos(x + dx, y + dy);
    }

    @Nullable
    public static Bounds bounds(Collection<GridPos> positions) {
        if (positions.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (GridPos pos : positions) {
            minX = Math.min(minX, pos.x);
            minY = Math.min(minY, pos.y);
            maxX = Math.max(maxX, pos.x);
            maxY = Math.max(maxY, pos.y);
        }
        return new Bounds(minX, minY, maxX, maxY);
    }

    public record Bounds(int minX, int minY, int maxX, int maxY) {
        public int width() {
            return maxX - minX + 1;
        }

        public int height() {
            return maxY - minY + 1;
        }

        public int rowMajorIndex(GridPos pos) {
            return (maxY - pos.y()) * width() + (pos.x() - minX);
        }
    }
}
