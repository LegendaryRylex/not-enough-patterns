package dev.rylex.nep.data;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;

final class MatrixCage {
    private MatrixCage() {}

    private static final int SIZE = 16;

    private static final float CENTRE = SIZE / 2.0F;

    /** Column 0 is the tone of a face looking straight at the core and the last interior column one that only grazes it, while rows are the animation frames. */
    private static final int RAMP_COLUMNS = SIZE;

    private static final int INTERIOR_COLUMNS = RAMP_COLUMNS / 2;

    /** The outward half of a skin, a spread of grain tones a box picks from by hash. */
    private static final int GRAIN_COLUMNS = RAMP_COLUMNS - INTERIOR_COLUMNS;

    /** Block light a face looking straight at the core is held at, scaled down by the same cosine the ramp column comes from. */
    private static final int GLOW_LIGHT = 15;

    /** Matches the opaque ring width of every {@code matrix/<set>/border.png}, so surface faces land on it exactly. */
    private static final int BAR = 2;

    private static final int FAR = SIZE - BAR;

    /** Cross-section of the struts between corners, set one pixel in from the surface so only the corners reach it. */
    private static final int STRUT = 1;

    private static final int STRUT_NEAR = BAR - STRUT;

    /** Depth of the voxel cut out of the very corner of each corner cube, leaving the seven that remain to be tiled by three boxes. */
    private static final int NOTCH = 1;

    /** Length of the band at the middle of every edge, skinned as a bar so the pulse carries through it rather than dying at it. */
    private static final int COLLAR = 2;

    private static final int COLLAR_MIN = (SIZE - COLLAR) / 2;
    private static final int COLLAR_MAX = COLLAR_MIN + COLLAR;

    private static final int FIELD_MIN = BAR;
    private static final int FIELD_MAX = SIZE - BAR - 1;

    /** One arm of the pattern, in cells of the twelve by twelve field inside the frame, turned three times about the face centre. */
    private static final int[][] ARM = {{3, 0}, {4, 0}, {2, 1}, {1, 2}, {0, 3}, {0, 4}};

    private static final int CORE_MIN = 5;
    private static final int CORE_MAX = 11;
    private static final float CORE_ANGLE = 45.0F;
    private static final float CELL = 2.0F;

    /** Offset of the port grids on both lateral axes, which lands their opening on a Create shaft's 6 to 10. */
    private static final int PORT_MIN = 4;

    /** The shaft port on each Y face, surface layer first, one row per z and one column per x. */
    private static final String[][] PORT_LAYERS = {
        {"........", "..XXXX..", ".X....X.", ".X....X.", ".X....X.", ".X....X.", "..XXXX..", "........"},
        {"..XXXX..", ".X....X.", "X......X", "X......X", "X......X", "X......X", ".X....X.", "..XXXX.."}
    };

    private static final int STUB_MIN = 6;
    private static final int STUB_MAX = 10;

    /** The gimbal rings sweep out to 6.15 px from the centre, so a stub stops 6.25 px out. */
    private static final float STUB_DEPTH = 1.75F;

    private static final List<Box> BOXES = boxes(false);
    private static final List<Box> PORTED_BOXES = boxes(true);

    /** A skin is a glow ramp across its columns, so a face's UVs carry its shading and nothing else. */
    static <T extends ModelBuilder<T>> void emitCage(
            ModelBuilder<T> model, String corner, String strut, String ring, boolean ported) {
        for (Box box : ported ? PORTED_BOXES : BOXES) {
            ModelBuilder<T>.ElementBuilder element = box.element(model);
            String skin =
                    switch (box.part()) {
                        case CORNER -> corner;
                        case STRUT -> strut;
                        case RING -> ring;
                    };
            for (Direction direction : Direction.values()) {
                if (box.hidden().contains(direction)) {
                    continue;
                }
                float[] uv = box.uvs(direction);
                var face = element.face(direction).texture(skin).uvs(uv[0], uv[1], uv[2], uv[3]);
                int light = box.glowLight(direction);
                if (light > 0) {
                    face.emissivity(light, 0);
                }
                if (box.onSurface(direction)) {
                    face.cullface(direction);
                }
                face.end();
            }
            element.end();
        }
    }

    static <T extends ModelBuilder<T>> void emitStaticCore(ModelBuilder<T> model, String texture) {
        int cell = 0;
        for (Direction.Axis axis : Direction.Axis.values()) {
            ModelBuilder<T>.ElementBuilder element = model.element()
                    .from(CORE_MIN, CORE_MIN, CORE_MIN)
                    .to(CORE_MAX, CORE_MAX, CORE_MAX)
                    .shade(false);
            float u = cell * CELL;
            for (Direction direction : Direction.values()) {
                element.face(direction)
                        .texture(texture)
                        .uvs(u, 0.0F, u + CELL, CELL)
                        .end();
            }
            element.rotation()
                    .origin(CENTRE, CENTRE, CENTRE)
                    .axis(axis)
                    .angle(CORE_ANGLE)
                    .end();
            element.end();
            cell++;
        }
    }

    static <T extends ModelBuilder<T>> void emitPortStubs(ModelBuilder<T> model, String side, String end) {
        for (float[] span : new float[][] {{0.0F, STUB_DEPTH}, {SIZE - STUB_DEPTH, SIZE}}) {
            ModelBuilder<T>.ElementBuilder element =
                    model.element().from(STUB_MIN, span[0], STUB_MIN).to(STUB_MAX, span[1], STUB_MAX);
            for (Direction direction : Direction.values()) {
                if (direction.getAxis() == Direction.Axis.Y) {
                    element.face(direction)
                            .texture(end)
                            .uvs(STUB_MIN, STUB_MIN, STUB_MAX, STUB_MAX)
                            .end();
                } else {
                    element.face(direction)
                            .texture(side)
                            .uvs(STUB_MIN, 0.0F, STUB_MAX, STUB_DEPTH)
                            .end();
                }
            }
            element.end();
        }
    }

    private static List<Box> boxes(boolean ported) {
        List<Box> boxes = new ArrayList<>();
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    notched(
                            boxes,
                            new int[] {x * FAR, y * FAR, z * FAR},
                            new int[] {x * FAR + BAR, y * FAR + BAR, z * FAR + BAR},
                            Direction.Axis.values(),
                            new int[] {x, y, z},
                            Part.CORNER);
                }
            }
        }
        for (Direction.Axis along : Direction.Axis.values()) {
            for (int p = 0; p < 2; p++) {
                for (int q = 0; q < 2; q++) {
                    edge(boxes, along, new int[] {p, q});
                }
            }
        }
        boolean[][] cells = ringCells();
        List<int[]> rectangles = ringRectangles(cells);
        for (Direction face : Direction.values()) {
            if (ported && face.getAxis() == Direction.Axis.Y) {
                continue;
            }
            for (int[] rectangle : rectangles) {
                boxes.add(ringBox(face, rectangle, cells));
            }
        }
        if (ported) {
            ports(boxes);
        }
        return List.copyOf(boxes);
    }

    /** An edge is two thin bars either side of a collar, and only the bars stop short of the block surface. */
    private static void edge(List<Box> boxes, Direction.Axis along, int[] sides) {
        Direction.Axis[] lateral = plane(along);
        int span = along.ordinal();
        int[] low = new int[3];
        int[] high = new int[3];
        for (int at = 0; at < 2; at++) {
            int axis = lateral[at].ordinal();
            low[axis] = sides[at] == 0 ? STRUT_NEAR : FAR;
            high[axis] = low[axis] + STRUT;
        }
        low[span] = BAR;
        high[span] = COLLAR_MIN;
        boxes.add(new Box(low[0], low[1], low[2], high[0], high[1], high[2], Part.STRUT, caps(along)));
        low[span] = COLLAR_MAX;
        high[span] = FAR;
        boxes.add(new Box(low[0], low[1], low[2], high[0], high[1], high[2], Part.STRUT, caps(along)));
        for (int at = 0; at < 2; at++) {
            int axis = lateral[at].ordinal();
            low[axis] = sides[at] == 0 ? 0 : FAR;
            high[axis] = low[axis] + BAR;
        }
        low[span] = COLLAR_MIN;
        high[span] = COLLAR_MAX;
        notched(boxes, low, high, lateral, sides, Part.STRUT);
    }

    /**
     * Tiles what is left of a box once the outermost voxel is cut away where the given axes meet, as one shell per
     * axis; a face the later shells cover only in part is emitted whole and buried behind them, so no two faces ever
     * land on the same plane.
     */
    private static void notched(List<Box> boxes, int[] low, int[] high, Direction.Axis[] cut, int[] sides, Part part) {
        int seed = Box.seed(low[0], low[1], low[2], high[0], high[1], high[2]);
        Set<Direction> covered = EnumSet.noneOf(Direction.class);
        for (int shell = 0; shell < cut.length; shell++) {
            int[] from = low.clone();
            int[] to = high.clone();
            for (int at = 0; at <= shell; at++) {
                int axis = cut[at].ordinal();
                if (sides[at] == 0) {
                    if (at < shell) {
                        to[axis] = low[axis] + NOTCH;
                    } else {
                        from[axis] = low[axis] + NOTCH;
                    }
                } else if (at < shell) {
                    from[axis] = high[axis] - NOTCH;
                } else {
                    to[axis] = high[axis] - NOTCH;
                }
            }
            boxes.add(new Box(from[0], from[1], from[2], to[0], to[1], to[2], part, Set.copyOf(covered), seed));
            covered.add(inward(cut[shell], sides[shell]));
        }
    }

    private static Direction inward(Direction.Axis axis, int side) {
        return Direction.fromAxisAndDirection(
                axis, side == 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
    }

    private static Set<Direction> caps(Direction.Axis axis) {
        return Set.of(
                Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE),
                Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
    }

    private static boolean[][] ringCells() {
        boolean[][] cells = new boolean[SIZE][SIZE];
        int[][] arm = new int[ARM.length][2];
        for (int at = 0; at < ARM.length; at++) {
            arm[at][0] = FIELD_MIN + ARM[at][0];
            arm[at][1] = FIELD_MIN + ARM[at][1];
        }
        for (int turn = 0; turn < 4; turn++) {
            for (int[] cell : arm) {
                cells[cell[0]][cell[1]] = true;
            }
            arm = turned(arm);
        }
        return cells;
    }

    private static int[][] turned(int[][] arm) {
        int[][] next = new int[arm.length][2];
        for (int at = 0; at < arm.length; at++) {
            next[at][0] = SIZE - 1 - arm[at][1];
            next[at][1] = arm[at][0];
        }
        return next;
    }

    private static List<int[]> ringRectangles(boolean[][] cells) {
        boolean[][] left = new boolean[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            left[x] = cells[x].clone();
        }
        List<int[]> rectangles = new ArrayList<>();
        for (int y = FIELD_MIN; y <= FIELD_MAX; y++) {
            for (int x = FIELD_MIN; x <= FIELD_MAX; x++) {
                if (!left[x][y]) {
                    continue;
                }
                int x1 = x;
                while (x1 + 1 <= FIELD_MAX && left[x1 + 1][y]) {
                    x1++;
                }
                int y1 = y;
                while (y1 + 1 <= FIELD_MAX && spans(left, x, x1, y1 + 1)) {
                    y1++;
                }
                for (int cx = x; cx <= x1; cx++) {
                    for (int cy = y; cy <= y1; cy++) {
                        left[cx][cy] = false;
                    }
                }
                rectangles.add(new int[] {x, y, x1, y1});
            }
        }
        return rectangles;
    }

    private static boolean spans(boolean[][] cells, int from, int to, int y) {
        for (int x = from; x <= to; x++) {
            if (!cells[x][y]) {
                return false;
            }
        }
        return true;
    }

    private static Box ringBox(Direction face, int[] rectangle, boolean[][] cells) {
        Direction.Axis normal = face.getAxis();
        Direction.Axis[] plane = plane(normal);
        int near = face.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? STRUT_NEAR : FAR;
        int[] from = new int[3];
        int[] to = new int[3];
        from[normal.ordinal()] = near;
        to[normal.ordinal()] = near + STRUT;
        from[plane[0].ordinal()] = rectangle[0];
        to[plane[0].ordinal()] = rectangle[2] + 1;
        from[plane[1].ordinal()] = rectangle[1];
        to[plane[1].ordinal()] = rectangle[3] + 1;
        Set<Direction> hidden = EnumSet.noneOf(Direction.class);
        for (int axis = 0; axis < 2; axis++) {
            for (Direction.AxisDirection sign : Direction.AxisDirection.values()) {
                if (buried(cells, rectangle, axis, sign)) {
                    hidden.add(Direction.fromAxisAndDirection(plane[axis], sign));
                }
            }
        }
        return new Box(from[0], from[1], from[2], to[0], to[1], to[2], Part.RING, hidden);
    }

    private static void ports(List<Box> boxes) {
        boolean[][][] cells = new boolean[SIZE][SIZE][SIZE];
        for (int layer = 0; layer < PORT_LAYERS.length; layer++) {
            String[] rows = PORT_LAYERS[layer];
            for (int y : new int[] {layer, SIZE - 1 - layer}) {
                for (int z = 0; z < rows.length; z++) {
                    for (int x = 0; x < rows[z].length(); x++) {
                        cells[PORT_MIN + x][y][PORT_MIN + z] = rows[z].charAt(x) == 'X';
                    }
                }
            }
        }
        for (int y = 0; y < SIZE; y++) {
            for (int z = 0; z < SIZE; z++) {
                for (int x = 0; x < SIZE; x++) {
                    if (!cells[x][y][z]) {
                        continue;
                    }
                    int end = x;
                    while (end + 1 < SIZE && cells[end + 1][y][z]) {
                        end++;
                    }
                    boxes.add(portBox(cells, x, end, y, z));
                    x = end;
                }
            }
        }
    }

    private static Box portBox(boolean[][][] cells, int from, int to, int y, int z) {
        Set<Direction> hidden = EnumSet.noneOf(Direction.class);
        for (Direction direction : Direction.values()) {
            boolean covered = true;
            for (int x = from; x <= to; x++) {
                covered &= filled(cells, x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ());
            }
            if (covered) {
                hidden.add(direction);
            }
        }
        return new Box(from, y, z, to + 1, y + 1, z + 1, Part.CORNER, hidden);
    }

    private static boolean filled(boolean[][][] cells, int x, int y, int z) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE && z >= 0 && z < SIZE && cells[x][y][z];
    }

    private static Direction.Axis[] plane(Direction.Axis normal) {
        return switch (normal) {
            case X -> new Direction.Axis[] {Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Y};
        };
    }

    private static boolean buried(boolean[][] cells, int[] rectangle, int axis, Direction.AxisDirection sign) {
        int line = sign == Direction.AxisDirection.POSITIVE ? rectangle[axis + 2] + 1 : rectangle[axis] - 1;
        for (int at = rectangle[1 - axis]; at <= rectangle[3 - axis]; at++) {
            int x = axis == 0 ? line : at;
            int y = axis == 0 ? at : line;
            boolean outside = x < FIELD_MIN || x > FIELD_MAX || y < FIELD_MIN || y > FIELD_MAX;
            if (!outside && !cells[x][y]) {
                return false;
            }
        }
        return true;
    }

    private enum Part {
        CORNER,
        STRUT,
        RING
    }

    private record Box(int x1, int y1, int z1, int x2, int y2, int z2, Part part, Set<Direction> hidden, int seed) {

        Box(int x1, int y1, int z1, int x2, int y2, int z2, Part part, Set<Direction> hidden) {
            this(x1, y1, z1, x2, y2, z2, part, hidden, seed(x1, y1, z1, x2, y2, z2));
        }

        /** Shared by every shell {@link #notched} cuts a node into, so a corner or a divider takes one grain tone instead of one per shell. */
        static int seed(int x1, int y1, int z1, int x2, int y2, int z2) {
            int hash = x1 * 73856093 ^ y1 * 19349663 ^ z1 * 83492791;
            hash = hash * 31 + (x2 * 6151 ^ y2 * 12289 ^ z2 * 24593);
            return hash ^ (hash >>> 13);
        }

        <T extends ModelBuilder<T>> ModelBuilder<T>.ElementBuilder element(ModelBuilder<T> model) {
            return model.element().from(x1, y1, z1).to(x2, y2, z2);
        }

        boolean onSurface(Direction direction) {
            int plane = plane(direction);
            return plane == 0 || plane == SIZE;
        }

        float[] uvs(Direction direction) {
            float column = glowColumn(direction);
            return new float[] {column, 0.0F, column + 1.0F, SIZE};
        }

        int glowLight(Direction direction) {
            return Math.round(Math.max(0.0F, glowCosine(direction)) * GLOW_LIGHT);
        }

        private int glowColumn(Direction direction) {
            float cosine = glowCosine(direction);
            if (cosine <= 0.0F) {
                return INTERIOR_COLUMNS + Math.floorMod(seed, GRAIN_COLUMNS);
            }
            int column = Math.round((1.0F - cosine) * (RAMP_COLUMNS - 1) / 2.0F);
            return Math.max(0, Math.min(INTERIOR_COLUMNS - 1, column));
        }

        private float glowCosine(Direction direction) {
            float nx = direction.getStepX();
            float ny = direction.getStepY();
            float nz = direction.getStepZ();
            float dx = CENTRE - (x1 + x2 + nx * (x2 - x1)) / 2.0F;
            float dy = CENTRE - (y1 + y2 + ny * (y2 - y1)) / 2.0F;
            float dz = CENTRE - (z1 + z2 + nz * (z2 - z1)) / 2.0F;
            float span = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            return span == 0.0F ? 0.0F : (nx * dx + ny * dy + nz * dz) / span;
        }

        private int plane(Direction direction) {
            return switch (direction) {
                case DOWN -> y1;
                case UP -> y2;
                case NORTH -> z1;
                case SOUTH -> z2;
                case WEST -> x1;
                case EAST -> x2;
            };
        }
    }
}
