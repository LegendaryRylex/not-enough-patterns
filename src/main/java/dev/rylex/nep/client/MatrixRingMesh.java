package dev.rylex.nep.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public final class MatrixRingMesh {

    public static final float TUBE = 0.25F;
    public static final float[] RADII = {4.30F, 5.10F, 5.90F};

    private static final int SIDES = 16;
    private static final int STRIDE = 8;
    private static final float PIXEL = 1.0F / 16.0F;
    private static final int RAMP_COLUMNS = 16;
    private static final int LIT_COLUMN = 0;

    /** The first of the cage skin's grain columns. */
    private static final int GRAIN_COLUMN = 8;

    private static final float[] AXIS_PLUS = {0.0F, 0.0F, 1.0F};
    private static final float[] AXIS_MINUS = {0.0F, 0.0F, -1.0F};

    /** Declared after every array it reads, since those are not compile time constants and would still be null during this initializer. */
    public static final MatrixRingMesh[] RINGS = rings();

    private final float[] data;

    private MatrixRingMesh(float[] data) {
        this.data = data;
    }

    /**
     * Builds a ring lying in the xy plane and pivoting about y, then applies {@code turns} cyclic axis
     * permutations, so turn one pivots about z and turn two about x.
     */
    public static MatrixRingMesh of(float radius, float tube, int turns) {
        float[] data = new float[SIDES * 4 * 4 * STRIDE];
        int grain = GRAIN_COLUMN + turns;
        int cursor = 0;
        float outer = (radius + tube) * PIXEL;
        float inner = (radius - tube) * PIXEL;
        float half = tube * PIXEL;
        for (int side = 0; side < SIDES; side++) {
            double first = angle(side);
            double second = angle(side + 1);
            float[] outerFirstPlus = point(first, outer, half);
            float[] outerFirstMinus = point(first, outer, -half);
            float[] outerSecondPlus = point(second, outer, half);
            float[] outerSecondMinus = point(second, outer, -half);
            float[] innerFirstPlus = point(first, inner, half);
            float[] innerFirstMinus = point(first, inner, -half);
            float[] innerSecondPlus = point(second, inner, half);
            float[] innerSecondMinus = point(second, inner, -half);
            float[] outward = radial(first, second, 1.0F);
            float[] inward = radial(first, second, -1.0F);
            cursor = quad(
                    data,
                    cursor,
                    turns,
                    grain,
                    outward,
                    outerFirstMinus,
                    outerSecondMinus,
                    outerSecondPlus,
                    outerFirstPlus);
            cursor = quad(
                    data,
                    cursor,
                    turns,
                    LIT_COLUMN,
                    inward,
                    innerFirstPlus,
                    innerSecondPlus,
                    innerSecondMinus,
                    innerFirstMinus);
            cursor = quad(
                    data,
                    cursor,
                    turns,
                    grain,
                    AXIS_PLUS,
                    outerFirstPlus,
                    outerSecondPlus,
                    innerSecondPlus,
                    innerFirstPlus);
            cursor = quad(
                    data,
                    cursor,
                    turns,
                    grain,
                    AXIS_MINUS,
                    innerFirstMinus,
                    innerSecondMinus,
                    outerSecondMinus,
                    outerFirstMinus);
        }
        return new MatrixRingMesh(data);
    }

    public void emit(PoseStack.Pose pose, VertexConsumer buffer, float brightness, int light, int overlay) {
        for (int index = 0; index < data.length; index += STRIDE) {
            buffer.addVertex(pose, data[index], data[index + 1], data[index + 2])
                    .setColor(brightness, brightness, brightness, 1.0F)
                    .setUv(data[index + 6], data[index + 7])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, data[index + 3], data[index + 4], data[index + 5]);
        }
    }

    private static int quad(float[] data, int cursor, int turns, int column, float[] normal, float[]... corners) {
        float u = (column + 0.5F) / RAMP_COLUMNS;
        float[] facing = turned(normal, turns);
        for (int corner = 0; corner < corners.length; corner++) {
            float[] position = turned(corners[corner], turns);
            data[cursor++] = position[0];
            data[cursor++] = position[1];
            data[cursor++] = position[2];
            data[cursor++] = facing[0];
            data[cursor++] = facing[1];
            data[cursor++] = facing[2];
            data[cursor++] = u;
            data[cursor++] = 0.0F;
        }
        return cursor;
    }

    private static MatrixRingMesh[] rings() {
        MatrixRingMesh[] built = new MatrixRingMesh[RADII.length];
        for (int ring = 0; ring < built.length; ring++) {
            built[ring] = of(RADII[ring], TUBE, ring);
        }
        return built;
    }

    private static double angle(int side) {
        return (side + 0.5) * Math.PI * 2.0 / SIDES;
    }

    private static float[] point(double angle, float radius, float axial) {
        return new float[] {(float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius, axial};
    }

    private static float[] radial(double first, double second, float sign) {
        float x = (float) (Math.cos(first) + Math.cos(second));
        float y = (float) (Math.sin(first) + Math.sin(second));
        float length = (float) Math.sqrt(x * x + y * y);
        return new float[] {sign * x / length, sign * y / length, 0.0F};
    }

    private static float[] turned(float[] vector, int turns) {
        float[] out = vector;
        for (int turn = 0; turn < turns; turn++) {
            out = new float[] {out[2], out[0], out[1]};
        }
        return out;
    }
}
