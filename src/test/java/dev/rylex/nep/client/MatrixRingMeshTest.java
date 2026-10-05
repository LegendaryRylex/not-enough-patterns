package dev.rylex.nep.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatrixRingMeshTest {

    private static final int SIDES = 16;
    private static final int FACES_PER_SIDE = 4;
    private static final int VERTICES_PER_QUAD = 4;
    private static final float PIXEL = 1.0F / 16.0F;

    /** The face plates of the cage reach this far in, so nothing rigid may pass it. */
    private static final float CLEARANCE = 6.2F * PIXEL;

    private static final float TOLERANCE = 1.0E-5F;

    @Test
    void everyRingIsAClosedTubeOfTheExpectedSize() {
        for (MatrixRingMesh ring : MatrixRingMesh.RINGS) {
            assertEquals(
                    SIDES * FACES_PER_SIDE * VERTICES_PER_QUAD,
                    emit(ring).positions.size());
        }
    }

    @Test
    void noRingReachesTheCageFacePlates() {
        assertTrue(
                outerReach(MatrixRingMesh.RINGS[MatrixRingMesh.RINGS.length - 1]) < CLEARANCE,
                "the outer ring sweeps into the cage");
    }

    @Test
    void noRingReachesTheCoreAtItsWidest() {
        assertTrue(
                innerReach(MatrixRingMesh.RINGS[0]) > MatrixCoreRenderer.CORE_REACH,
                "the inner ring cuts the core when it pulses");
    }

    @Test
    void ringsNeverShareAShell() {
        for (int ring = 1; ring < MatrixRingMesh.RINGS.length; ring++) {
            assertTrue(
                    innerReach(MatrixRingMesh.RINGS[ring]) > outerReach(MatrixRingMesh.RINGS[ring - 1]),
                    "rings " + (ring - 1) + " and " + ring + " can collide");
        }
    }

    @Test
    void everyRingLiesInItsOwnPlane() {
        for (int ring = 0; ring < MatrixRingMesh.RINGS.length; ring++) {
            int flat = (ring + 2) % 3;
            for (float[] position : emit(MatrixRingMesh.RINGS[ring]).positions) {
                assertEquals(
                        MatrixRingMesh.TUBE * PIXEL,
                        Math.abs(position[flat]),
                        TOLERANCE,
                        "ring " + ring + " is not flat on its own axis");
            }
        }
    }

    @Test
    void everyNormalIsAUnitVector() {
        for (MatrixRingMesh ring : MatrixRingMesh.RINGS) {
            for (float[] normal : emit(ring).normals) {
                assertEquals(1.0F, length(normal), TOLERANCE, "normal is not unit length");
            }
        }
    }

    private static float outerReach(MatrixRingMesh ring) {
        float reach = 0.0F;
        for (float[] position : emit(ring).positions) {
            reach = Math.max(reach, length(position));
        }
        return reach;
    }

    private static float innerReach(MatrixRingMesh ring) {
        List<float[]> positions = emit(ring).positions;
        float reach = Float.MAX_VALUE;
        for (int corner = 0; corner < positions.size(); corner += VERTICES_PER_QUAD) {
            float[] centre = new float[3];
            for (int vertex = 0; vertex < VERTICES_PER_QUAD; vertex++) {
                for (int axis = 0; axis < 3; axis++) {
                    centre[axis] += positions.get(corner + vertex)[axis] / VERTICES_PER_QUAD;
                }
            }
            reach = Math.min(reach, length(centre));
        }
        return reach;
    }

    private static Capture emit(MatrixRingMesh ring) {
        Capture capture = new Capture();
        ring.emit(new PoseStack().last(), capture, 1.0F, 0, 0);
        return capture;
    }

    private static float length(float[] vector) {
        return (float) Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]);
    }

    private static final class Capture implements VertexConsumer {

        private final List<float[]> positions = new ArrayList<>();
        private final List<float[]> normals = new ArrayList<>();

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
