package Programs;

import GxEngine3D.Camera.Camera;
import GxEngine3D.Helper.Maths.FrustumMatrixHelper;
import GxEngine3D.Helper.Maths.MatrixHelper;
import GxEngine3D.Model.Matrix.Matrix;

import java.util.Arrays;

//testing of the idea that applying the inverse matrix to the bary point in clip-space is equivalent to
//applying the inverse matrix to the point in clip-space then converting to bary point relative-space
public class InverseBaryCheat {

    Matrix allCombined, inverse;

    double[][] relativePoly, clipPoly, screenPoly, relativeTexturePoly;

    public static double[] avg(double[][] points)
    {
        double[] avg = new double[points[0].length];

        for (double[] d:points)
        {
            for (int i=0;i<avg.length;i++) {
                avg[i] += d[i];
            }
        }

        for (int i=0;i<avg.length;i++)
        {
            avg[i] /= points.length;
        }

        return avg;
    }

    public static void main(String args[])
    {
        InverseBaryCheat i = new InverseBaryCheat();

        i.setup();

        System.out.println("Regular: " + Arrays.toString(i.regularMethod(i.regularPoint())));

        System.out.println("Cheat: " + Arrays.toString(i.cheatMethod(i.cheatPoint())));
    }

    public double[] regularPoint()
    {
        return avg(clipPoly);
    }

    public double[] cheatPoint()
    {
        return avg(clipPoly);
    }

    public void setup()
    {
        createMatrix();

        addPolygons();
    }

    public void createMatrix()
    {
        double[][] translate = MatrixHelper.setupTranslateMatrix(0, 0, 0);
        double[][] rotate = MatrixHelper.setupFullRotation(0, 0, 0);
        double[][] scale = MatrixHelper.setupTranslateMatrix(0, 0, 0);

        Matrix combined = new Matrix(translate);
        combined = new Matrix(combined.matrixMultiply(rotate));
        combined = new Matrix(combined.matrixMultiply(scale));

        Camera camera = new Camera(0,0, 2);
        camera.setup();

        Matrix frustum = FrustumMatrixHelper.createMatrix(75, 0.1, 30, 500, 500, 1);
        Matrix projection = new Matrix(frustum.matrixMultiply(camera.getMatrix()));

        this.allCombined = new Matrix(combined.matrixMultiply(projection));

        this.inverse = new Matrix(this.allCombined.inverse_4x4());
    }

    private void addPolygons()
    {
        double[][] relativePoints = new double[][]{
                new double[]{0, 0, 0},
                new double[]{1, 0, 0},
                new double[]{1, 1, 0},
        };

        this.relativePoly = relativePoints;
        this.relativeTexturePoly = relativePoints;

        this.clipPoly = new double[relativePoints.length][];
        for (int i=0;i<relativePoints.length;i++) {
            this.clipPoly[i] = MatrixHelper.applyExplicitMatrix(allCombined, relativePoints[i]);
        }

        this.screenPoly = new double[clipPoly.length][2];
        for (int i=0;i<clipPoly.length;i++) {
            this.screenPoly[i][0] = (this.clipPoly[i][0] + 1) * 0.5 * 500;
            this.screenPoly[i][1] = (1 - (this.clipPoly[i][1] + 1) * 0.5) * 500;
        }
    }

    public double[] findBarycentricPoint(double[][] t, double[] point)
    {
        double denominator = (t[1][1] - t[2][1])*(t[0][0] - t[2][0]) + (t[2][0] - t[1][0])*(t[0][1] - t[2][1]);
        double v = ((t[1][1] - t[2][1])*(point[0] - t[2][0]) + (t[2][0] - t[1][0])*(point[1] - t[2][1])) / denominator;
        double w = ((t[2][1] - t[0][1])*(point[0] - t[2][0]) + (t[0][0] - t[2][0])*(point[1] - t[2][1])) / denominator;

        return new double[]{
                1 - v - w, v, w
        };
    }

    public double[] applyBary(double[] bary, double[][] triangle)
    {
        double[] point = {
                (triangle[0][0] * bary[0]) + (triangle[1][0] * bary[1]) + (triangle[2][0] * bary[2]),
                (triangle[0][1] * bary[0]) + (triangle[1][1] * bary[1]) + (triangle[2][1] * bary[2])
        };
        return point;
    }

    //this method is known to work and requires 3 pieces of additional information:
    //      inverse matrix that would convert clip=>relative,
    //      relative polygon,
    //      relative texture polygon
    public double[] regularMethod(double[] point)
    {
        //take clip points, apply inverse
        double[] inversePoint = MatrixHelper.applyExplicitMatrix(inverse, point);
        //find barycentric point using result and relative points
        double[] bary = findBarycentricPoint(relativePoly, inversePoint);
        //apply barycentric point to texture relative poly
        return applyBary(bary, relativeTexturePoly);
    }

    //this method doesn't work and would require 2 pieces of additional information
    //      inverse matrix,
    //      relative texture polygon
    //this would've resulted in less memory to write and handle
    public double[] cheatMethod(double[] point)
    {
        //find bary point from clip points
        double[] bary = findBarycentricPoint(clipPoly, point);
        //apply inverse to the bary points instead
        double[] inversePoint = MatrixHelper.applyExplicitMatrix(inverse, bary);
        //apply inverse bary points to the texture poly
        return applyBary(inversePoint, relativeTexturePoly);
    }
}
