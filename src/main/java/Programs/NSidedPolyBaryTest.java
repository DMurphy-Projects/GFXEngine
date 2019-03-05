package Programs;
import GxEngine3D.Helper.DistanceCalc;
import GxEngine3D.Helper.VectorCalc;

import java.util.Arrays;

public class NSidedPolyBaryTest {

    public static void main(String[] args)
    {
        double[][] poly = new double[][]{
                new double[]{0, 0},
                new double[]{1, 0},
                new double[]{1, 1},
                new double[]{0.5, 1.5},
                new double[]{0, 1},
        };

        double[] point = new double[]{.5, .5};

        int numberOfPossibleVectors = poly.length-2;

        double[][] v0 = new double[numberOfPossibleVectors][2];
        double[][] v1 = new double[numberOfPossibleVectors][2];

        double[] dot00 = new double[numberOfPossibleVectors];
        double[] dot01 = new double[numberOfPossibleVectors];
        double[] dot11 = new double[numberOfPossibleVectors];

        double[] invDenom = new double[numberOfPossibleVectors];

        double[] weights = new double[3];
        int[] triangleIndex = new int[3];

        //pre computed values
        int noTriangles = poly.length - 2;
        for (int i=0;i<noTriangles;i++)
        {
            //A is 0, B is i1, C is i2
            v0[i] = VectorCalc.sub(poly[i+2], poly[0]);//C-A
            v1[i] = VectorCalc.sub(poly[i+1], poly[0]);//B-A

            dot00[i] = VectorCalc.dot(v0[i], v0[i]);
            dot01[i] = VectorCalc.dot(v0[i], v1[i]);
            dot11[i] = VectorCalc.dot(v1[i], v1[i]);

            invDenom[i] = (dot00[i] * dot11[i]) - (dot01[i] * dot01[i]);
        }

        //calc if point is in polygon, and which one
        for (int i=0;i<noTriangles;i++)
        {
            double[] v2 = VectorCalc.sub(point, poly[0]);//P-A
            double dot02 = VectorCalc.dot(v0[i], v2);
            double dot12 = VectorCalc.dot(v1[i], v2);

            double v = ((dot00[i] * dot12) - (dot01[i] * dot02)) / invDenom[i];
            double w = ((dot11[i] * dot02) - (dot01[i] * dot12)) / invDenom[i];

            //test if inside triangle
            if ((w >= 0) && (v >= 0) && (w + v < 1))
            {
                weights[0] = 1 - w - v;
                weights[1] = v;
                weights[2] = w;

                triangleIndex[0] = 0;
                triangleIndex[1] = i+1;
                triangleIndex[2] = i+2;
                break;
            }
        }

        System.out.println("Weights "+Arrays.toString(weights));
        System.out.println("Triangle Index: "+Arrays.toString(triangleIndex));

        double[] sanity = new double[2];
        for (int i=0;i<weights.length;i++)
        {
            sanity[0] += weights[i] * poly[triangleIndex[i]][0];
            sanity[1] += weights[i] * poly[triangleIndex[i]][1];
        }

        System.out.println("Starting Value: "+Arrays.toString(point));
        System.out.println("Sanity Check: "+Arrays.toString(sanity));
    }
}
