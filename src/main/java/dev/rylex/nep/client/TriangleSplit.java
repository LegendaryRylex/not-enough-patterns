package dev.rylex.nep.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;

final class TriangleSplit {
    private TriangleSplit() {}

    static List<double[][]> once(List<double[][]> triangles, BinaryOperator<double[]> midpoint) {
        List<double[][]> split = new ArrayList<>(triangles.size() * 4);
        for (double[][] triangle : triangles) {
            double[] a = triangle[0];
            double[] b = triangle[1];
            double[] c = triangle[2];
            double[] ab = midpoint.apply(a, b);
            double[] bc = midpoint.apply(b, c);
            double[] ca = midpoint.apply(c, a);
            split.add(new double[][] {a, ab, ca});
            split.add(new double[][] {ab, b, bc});
            split.add(new double[][] {ca, bc, c});
            split.add(new double[][] {ab, bc, ca});
        }
        return split;
    }
}
