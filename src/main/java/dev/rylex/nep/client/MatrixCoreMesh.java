package dev.rylex.nep.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;

public final class MatrixCoreMesh {

    public static final MatrixCoreMesh SPHERE = sphere();

    private static final int STRIDE = 8;
    private static final int FACES = 20;
    private static final int SUBDIVISIONS = 2;
    private static final double EDGE = 2.0;
    private static final double EDGE_EPSILON = 1.0E-6;
    private static final double POLE_EPSILON = 1.0E-6;
    private static final double TAU = Math.PI * 2.0;

    private final float[] data;

    private MatrixCoreMesh(float[] data) {
        this.data = data;
    }

    public void emit(
            PoseStack.Pose pose, VertexConsumer buffer, float brightness, float alpha, int light, int overlay) {
        for (int i = 0; i < data.length; i += STRIDE) {
            buffer.addVertex(pose, data[i], data[i + 1], data[i + 2])
                    .setColor(brightness, brightness, brightness, alpha)
                    .setUv(data[i + 6], data[i + 7])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, data[i + 3], data[i + 4], data[i + 5]);
        }
    }

    /** Triangles on the unit sphere, in the face order the sheet's cells are laid out in. */
    static List<double[][]> unitFaces() {
        double[][] points = vertexTable();
        List<int[]> triples = facesByEdgeLength(points);
        if (triples.size() != FACES) {
            throw new IllegalStateException("icosahedron vertex table yielded " + triples.size() + " faces, not 20");
        }
        double radius = length(points[0]);
        List<double[][]> faces = new ArrayList<>(triples.size());
        for (int[] triple : triples) {
            faces.add(new double[][] {
                scale(points[triple[0]], 1.0 / radius),
                scale(points[triple[1]], 1.0 / radius),
                scale(points[triple[2]], 1.0 / radius)
            });
        }
        return faces;
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {
            a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0],
        };
    }

    private static double[] subtract(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] normalise(double[] a) {
        return scale(a, 1.0 / length(a));
    }

    private static MatrixCoreMesh sphere() {
        List<double[][]> faces = unitFaces();
        List<double[][]> patches = patches();
        float[] data = new float[faces.size() * patches.size() * 4 * STRIDE];
        int cursor = 0;
        for (double[][] triangle : faces) {
            for (double[][] patch : patches) {
                cursor = facet(data, cursor, triangle, patch);
            }
        }
        return new MatrixCoreMesh(data);
    }

    /** Subdivision runs in barycentric weights rather than positions, so a patch stays on its parent's arc. */
    private static List<double[][]> patches() {
        List<double[][]> patches = new ArrayList<>();
        patches.add(new double[][] {{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}});
        for (int pass = 0; pass < SUBDIVISIONS; pass++) {
            List<double[][]> split = new ArrayList<>(patches.size() * 4);
            for (double[][] patch : patches) {
                double[] a = patch[0];
                double[] b = patch[1];
                double[] c = patch[2];
                double[] ab = mean(a, b);
                double[] bc = mean(b, c);
                double[] ca = mean(c, a);
                split.add(new double[][] {a, ab, ca});
                split.add(new double[][] {ab, b, bc});
                split.add(new double[][] {ca, bc, c});
                split.add(new double[][] {ab, bc, ca});
            }
            patches = split;
        }
        return patches;
    }

    private static double[] mean(double[] a, double[] b) {
        return new double[] {(a[0] + b[0]) / 2.0, (a[1] + b[1]) / 2.0, (a[2] + b[2]) / 2.0};
    }

    /** Emits one triangle as a degenerate quad, since the buffer format has no triangle mode. */
    private static int facet(float[] data, int cursor, double[][] triangle, double[][] patch) {
        double[][] corner = {
            normalise(blend(triangle, patch[0])),
            normalise(blend(triangle, patch[1])),
            normalise(blend(triangle, patch[2]))
        };
        float[][] uv = unwrapped(corner);
        cursor = vertex(data, cursor, corner[0], uv[0]);
        cursor = vertex(data, cursor, corner[1], uv[1]);
        cursor = vertex(data, cursor, corner[2], uv[2]);
        return vertex(data, cursor, corner[2], uv[2]);
    }

    /** Shifts trailing longitudes onto the anchor's turn so a seam-straddling triangle interpolates across its own width, and gives a pole corner its triangle's mean longitude in place of the arbitrary one {@code atan2} returns on the axis. */
    private static float[][] unwrapped(double[][] corner) {
        float[][] uv = new float[corner.length][];
        for (int i = 0; i < corner.length; i++) {
            uv[i] = project(corner[i]);
        }
        int anchor = isPole(corner[0]) ? 1 : 0;
        for (int i = 0; i < corner.length; i++) {
            if (i != anchor) {
                uv[i][0] += Math.round(uv[anchor][0] - uv[i][0]);
            }
        }
        for (int i = 0; i < corner.length; i++) {
            if (isPole(corner[i])) {
                uv[i][0] = (uv[(i + 1) % corner.length][0] + uv[(i + 2) % corner.length][0]) * 0.5F;
            }
        }
        return uv;
    }

    /** Turns about x, which is neither shell layer's axis, so no two of the three pole pinches coincide. */
    private static float[] project(double[] point) {
        double azimuth = Math.atan2(point[1], point[2]);
        double polar = Math.max(-1.0, Math.min(1.0, point[0]));
        return new float[] {(float) (azimuth / TAU + 0.5), (float) (Math.asin(polar) / Math.PI + 0.5)};
    }

    private static boolean isPole(double[] point) {
        return Math.abs(point[0]) > 1.0 - POLE_EPSILON;
    }

    private static int vertex(float[] data, int cursor, double[] position, float[] uv) {
        data[cursor] = (float) position[0];
        data[cursor + 1] = (float) position[1];
        data[cursor + 2] = (float) position[2];
        data[cursor + 3] = (float) position[0];
        data[cursor + 4] = (float) position[1];
        data[cursor + 5] = (float) position[2];
        data[cursor + 6] = uv[0];
        data[cursor + 7] = uv[1];
        return cursor + STRIDE;
    }

    private static double[] blend(double[][] triangle, double[] weights) {
        double[] blended = new double[3];
        for (int axis = 0; axis < 3; axis++) {
            blended[axis] =
                    triangle[0][axis] * weights[0] + triangle[1][axis] * weights[1] + triangle[2][axis] * weights[2];
        }
        return blended;
    }

    private static double[][] vertexTable() {
        double phi = (1.0 + Math.sqrt(5.0)) / 2.0;
        double[][] points = new double[12][];
        int index = 0;
        for (int first = -1; first <= 1; first += 2) {
            for (int second = -1; second <= 1; second += 2) {
                points[index++] = new double[] {0.0, first, second * phi};
                points[index++] = new double[] {first, second * phi, 0.0};
                points[index++] = new double[] {first * phi, 0.0, second};
            }
        }
        return points;
    }

    private static List<int[]> facesByEdgeLength(double[][] points) {
        List<int[]> faces = new ArrayList<>();
        for (int a = 0; a < points.length; a++) {
            for (int b = a + 1; b < points.length; b++) {
                if (!isEdge(points[a], points[b])) {
                    continue;
                }
                for (int c = b + 1; c < points.length; c++) {
                    if (isEdge(points[a], points[c]) && isEdge(points[b], points[c])) {
                        faces.add(wound(points, a, b, c));
                    }
                }
            }
        }
        return faces;
    }

    private static int[] wound(double[][] points, int a, int b, int c) {
        double[] normal = cross(subtract(points[b], points[a]), subtract(points[c], points[a]));
        return dot(normal, centroid(points[a], points[b], points[c])) < 0.0 ? new int[] {a, c, b} : new int[] {a, b, c};
    }

    private static boolean isEdge(double[] a, double[] b) {
        return Math.abs(distance(a, b) - EDGE) < EDGE_EPSILON;
    }

    private static double distance(double[] a, double[] b) {
        double[] delta = subtract(a, b);
        return Math.sqrt(dot(delta, delta));
    }

    private static double[] centroid(double[] a, double[] b, double[] c) {
        return new double[] {(a[0] + b[0] + c[0]) / 3.0, (a[1] + b[1] + c[1]) / 3.0, (a[2] + b[2] + c[2]) / 3.0};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] scale(double[] a, double factor) {
        return new double[] {a[0] * factor, a[1] * factor, a[2] * factor};
    }

    private static double length(double[] a) {
        return Math.sqrt(dot(a, a));
    }
}
