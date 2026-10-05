package dev.rylex.nep.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;

public final class MatrixCoreShell {

    public enum Layer {
        BROAD(3),
        FINE(5);

        private final int offset;

        Layer(int offset) {
            this.offset = offset;
        }
    }

    public static final MatrixCoreShell SPHERE = sphere();

    private static final int STRIDE = 7;
    private static final int SUBDIVISIONS = 1;
    private static final double TAU = Math.PI * 2.0;

    private final float[] data;

    private MatrixCoreShell(float[] data) {
        this.data = data;
    }

    /**
     * Additive blending scales by alpha alone, so {@code strength} is faded per vertex by how squarely it
     * faces the camera, which lets the glow trail off instead of ending on the shell's polygon silhouette.
     */
    public void emit(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            Layer layer,
            int tint,
            float strength,
            float tiling,
            float uScroll,
            float vScroll,
            int light,
            int overlay) {
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        Vector3f facing = new Vector3f();
        for (int i = 0; i < data.length; i += STRIDE) {
            pose.transformNormal(data[i], data[i + 1], data[i + 2], facing);
            float front = Math.max(0.0F, facing.z());
            buffer.addVertex(pose, data[i], data[i + 1], data[i + 2])
                    .setColor(red, green, blue, strength * front * front)
                    .setUv(data[i + layer.offset] * tiling + uScroll, data[i + layer.offset + 1] * tiling + vScroll)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, data[i], data[i + 1], data[i + 2]);
        }
    }

    private static MatrixCoreShell sphere() {
        List<double[][]> faces = MatrixCoreMesh.unitFaces();
        for (int pass = 0; pass < SUBDIVISIONS; pass++) {
            faces = subdivide(faces);
        }
        float[] data = new float[faces.size() * 4 * STRIDE];
        int cursor = 0;
        for (double[][] triangle : faces) {
            cursor = facet(data, cursor, triangle[0], triangle[1], triangle[2]);
        }
        return new MatrixCoreShell(data);
    }

    private static List<double[][]> subdivide(List<double[][]> faces) {
        List<double[][]> out = new ArrayList<>(faces.size() * 4);
        for (double[][] triangle : faces) {
            double[] a = triangle[0];
            double[] b = triangle[1];
            double[] c = triangle[2];
            double[] ab = midpoint(a, b);
            double[] bc = midpoint(b, c);
            double[] ca = midpoint(c, a);
            out.add(new double[][] {a, ab, ca});
            out.add(new double[][] {ab, b, bc});
            out.add(new double[][] {ca, bc, c});
            out.add(new double[][] {ab, bc, ca});
        }
        return out;
    }

    private static double[] midpoint(double[] a, double[] b) {
        return MatrixCoreMesh.normalise(new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]});
    }

    /** Emits one triangle as a degenerate quad, since the buffer format has no triangle mode. */
    private static int facet(float[] data, int cursor, double[] a, double[] b, double[] c) {
        float[][] broad = projected(a, b, c, Layer.BROAD);
        float[][] fine = projected(a, b, c, Layer.FINE);
        cursor = vertex(data, cursor, a, broad[0], fine[0]);
        cursor = vertex(data, cursor, b, broad[1], fine[1]);
        cursor = vertex(data, cursor, c, broad[2], fine[2]);
        return vertex(data, cursor, c, broad[2], fine[2]);
    }

    /**
     * Shifts the trailing longitudes onto the first vertex's turn, so a triangle straddling the
     * projection wrap interpolates across its own width instead of backwards over the whole texture.
     */
    private static float[][] projected(double[] a, double[] b, double[] c, Layer layer) {
        float[][] uv = {project(a, layer), project(b, layer), project(c, layer)};
        uv[1][0] += Math.round(uv[0][0] - uv[1][0]);
        uv[2][0] += Math.round(uv[0][0] - uv[2][0]);
        return uv;
    }

    /** The two layers project about different axes, so neither one's pole pinch lands on the other's. */
    private static float[] project(double[] point, Layer layer) {
        double azimuth = layer == Layer.BROAD ? Math.atan2(point[2], point[0]) : Math.atan2(point[0], point[1]);
        double polar = layer == Layer.BROAD ? point[1] : point[2];
        return new float[] {
            (float) (azimuth / TAU + 0.5), (float) (Math.asin(Math.max(-1.0, Math.min(1.0, polar))) / Math.PI + 0.5)
        };
    }

    private static int vertex(float[] data, int cursor, double[] position, float[] broad, float[] fine) {
        data[cursor] = (float) position[0];
        data[cursor + 1] = (float) position[1];
        data[cursor + 2] = (float) position[2];
        data[cursor + 3] = broad[0];
        data[cursor + 4] = broad[1];
        data[cursor + 5] = fine[0];
        data[cursor + 6] = fine[1];
        return cursor + STRIDE;
    }
}
