package dev.rylex.nep.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MatrixCoreMeshTest {

    private static final int SPHERE_FACES = 320;
    private static final int SPHERE_VERTICES = 162;
    private static final int POLE_VERTICES = 12;
    private static final int VERTICES_PER_QUAD = 4;
    private static final int CORNERS_PER_QUAD = 3;
    private static final float CIRCUMRADIUS = 1.0F;
    private static final float TOLERANCE = 1.0E-4F;
    private static final float POLE = 1.0F - 1.0E-4F;
    private static final float SEAM_SPAN = 0.5F;
    private static final double TAU = Math.PI * 2.0;

    @Test
    void emitsOneQuadForEverySubdividedFace() {
        Capture capture = emit();
        assertEquals(SPHERE_FACES * VERTICES_PER_QUAD, capture.positions.size());
    }

    @Test
    void everyVertexSitsOnTheCircumsphere() {
        for (float[] position : emit().positions) {
            assertEquals(CIRCUMRADIUS, length(position), TOLERANCE, "vertex is off the circumsphere");
        }
    }

    @Test
    void everyFaceIsDrawnFromTheSharedCorners() {
        Set<String> corners = new HashSet<>();
        for (float[] position : emit().positions) {
            corners.add(round(position[0]) + "," + round(position[1]) + "," + round(position[2]));
        }
        assertEquals(SPHERE_VERTICES, corners.size());
    }

    @Test
    void everyNormalIsUnitLengthAndRadial() {
        Capture capture = emit();
        for (int i = 0; i < capture.positions.size(); i++) {
            float[] normal = capture.normals.get(i);
            assertEquals(1.0F, length(normal), TOLERANCE, "normal is not unit length");
            assertEquals(1.0F, dot(normal, capture.positions.get(i)), TOLERANCE, "normal is not radial at vertex " + i);
        }
    }

    @Test
    void everyLatitudeIsTheProjectionOfItsVertex() {
        Capture capture = emit();
        for (int i = 0; i < capture.positions.size(); i++) {
            float x = capture.positions.get(i)[0];
            float expected = (float) (Math.asin(Math.max(-1.0F, Math.min(1.0F, x))) / Math.PI + 0.5);
            assertEquals(expected, capture.uvs.get(i)[1], TOLERANCE, "latitude does not match vertex " + i);
        }
    }

    @Test
    void everyOffAxisLongitudeIsTheProjectionOfItsVertexUpToAWholeTurn() {
        Capture capture = emit();
        for (int i = 0; i < capture.positions.size(); i++) {
            float[] position = capture.positions.get(i);
            if (Math.abs(position[0]) > POLE) {
                continue;
            }
            float expected = (float) (Math.atan2(position[1], position[2]) / TAU + 0.5);
            float turns = capture.uvs.get(i)[0] - expected;
            assertEquals(Math.round(turns), turns, TOLERANCE, "longitude does not match vertex " + i);
        }
    }

    @Test
    void noTriangleStraddlesTheSeam() {
        Capture capture = emit();
        for (int quad = 0; quad < capture.uvs.size(); quad += VERTICES_PER_QUAD) {
            float low = Float.MAX_VALUE;
            float high = -Float.MAX_VALUE;
            for (int corner = 0; corner < CORNERS_PER_QUAD; corner++) {
                float u = capture.uvs.get(quad + corner)[0];
                low = Math.min(low, u);
                high = Math.max(high, u);
            }
            assertTrue(high - low < SEAM_SPAN, "triangle " + quad / VERTICES_PER_QUAD + " wraps the long way round");
        }
    }

    @Test
    void everyPoleVertexTakesItsTriangleMeanLongitude() {
        Capture capture = emit();
        int poles = 0;
        for (int quad = 0; quad < capture.positions.size(); quad += VERTICES_PER_QUAD) {
            for (int corner = 0; corner < CORNERS_PER_QUAD; corner++) {
                if (Math.abs(capture.positions.get(quad + corner)[0]) < POLE) {
                    continue;
                }
                poles++;
                float other = capture.uvs.get(quad + (corner + 1) % CORNERS_PER_QUAD)[0];
                float third = capture.uvs.get(quad + (corner + 2) % CORNERS_PER_QUAD)[0];
                assertEquals(
                        (other + third) * 0.5F,
                        capture.uvs.get(quad + corner)[0],
                        TOLERANCE,
                        "pole vertex kept the arbitrary longitude atan2 returns on the axis");
            }
        }
        assertEquals(POLE_VERTICES, poles);
    }

    private static Capture emit() {
        Capture capture = new Capture();
        MatrixCoreMesh.SPHERE.emit(new PoseStack().last(), capture, 1.0F, 1.0F, 0, 0);
        return capture;
    }

    private static int round(float coordinate) {
        return Math.round(coordinate * 1.0E4F);
    }

    private static float length(float[] vector) {
        return (float) Math.sqrt(dot(vector, vector));
    }

    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static final class Capture implements VertexConsumer {

        private final List<float[]> positions = new ArrayList<>();
        private final List<float[]> normals = new ArrayList<>();
        private final List<float[]> uvs = new ArrayList<>();

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            positions.add(new float[] {x, y, z});
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            normals.add(new float[] {x, y, z});
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            uvs.add(new float[] {u, v});
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int argb) {
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }
}
