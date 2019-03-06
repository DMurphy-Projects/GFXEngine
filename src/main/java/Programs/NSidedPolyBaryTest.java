package Programs;
import GxEngine3D.Helper.VectorCalc;

import java.util.Arrays;

public class NSidedPolyBaryTest {

    public static void main(String[] args)
    {
        double[][] poly = new double[][]{
                new double[]{0, 0, 1},
                new double[]{1, 0, 1},
                new double[]{1, 1, 1},
                new double[]{0.5, 1.5, 1},
                new double[]{0, 1, 1},
        };

        double[] point = new double[]{.5, .2, 1};

        int noTriangles = poly.length - 2;
        double[][] vectors = new double[poly.length-1][];

        int size = 4;//how many arrays do we want to combine
        double[] preCalc = new double[noTriangles * size];//technically not just dot products anymore

        double[] weights = new double[3];
        int[] triangleIndex = new int[3];

        //---pre computed values start
        for (int i=0;i<poly.length-1;i++)
        {
            vectors[i] = VectorCalc.sub(poly[i+1], poly[0]);//all vectors towards poly[0], this is both (C-A, B-A)
        }

        for (int i=0;i<noTriangles;i++)
        {
            preCalc[i*size] = VectorCalc.dot(vectors[i+1], vectors[i+1]);//dot00
            preCalc[i*size+1] = VectorCalc.dot(vectors[i+1], vectors[i]);//dot01
            preCalc[i*size+2] = VectorCalc.dot(vectors[i], vectors[i]);//dot11
            preCalc[i*size+3] = (preCalc[i*size] * preCalc[i*size+2]) - (preCalc[i*size+1] * preCalc[i*size+1]);//denominator
        }
        //---pre computed values end

        //calc if point is in polygon, and which one
        for (int i=0;i<noTriangles;i++)
        {
            double[] v2 = VectorCalc.sub(point, poly[0]);//P-A
            double dot02 = VectorCalc.dot(vectors[i+1], v2);
            double dot12 = VectorCalc.dot(vectors[i], v2);

            double v = ((preCalc[i*size] * dot12) - (preCalc[i*size+1] * dot02)) / preCalc[i*size+3];//(dot00*dot12 - dot01*dot02) / denom
            double w = ((preCalc[i*size+2] * dot02) - (preCalc[i*size+1] * dot12)) / preCalc[i*size+3];//(dot11*dot02) - (dot01*dot12) / denom

            //test if inside triangle
            if ((w >= 0) && (v >= 0) && (w + v < 1))
            {
                weights[0] = 1 - w - v;//u
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

        double[] sanity = new double[3];
        for (int i=0;i<weights.length;i++)
        {
            sanity[0] += weights[i] * poly[triangleIndex[i]][0];
            sanity[1] += weights[i] * poly[triangleIndex[i]][1];
            sanity[2] += weights[i] * poly[triangleIndex[i]][2];
        }

        System.out.println("Starting Value: "+Arrays.toString(point));
        System.out.println("Sanity Check: "+Arrays.toString(sanity));
    }
}
