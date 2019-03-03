package Programs;

import DebugTools.TextModule.NullImplement;
import DebugTools.TextOutput;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Helper.FrustumMatrixHelper;
import GxEngine3D.Helper.MatrixHelper;
import GxEngine3D.Model.Matrix.Matrix;

import java.util.Arrays;

public class InverseMatrixTest {

    public static void main(String[] args)
    {
        //---real matrix setup start
        Camera c = new Camera(0, 0, 0);
        c.setup();
        Matrix frustum = FrustumMatrixHelper.createMatrix(90, 0.1, 30, 100, 100, 1);
        Matrix projection = new Matrix(frustum.matrixMultiply(c.getMatrix()));
        //---real matrix setup end
        double[][] inverseTestData = new double[][]{
                new double[]{1, 1, 1, -1},
                new double[]{1, 1, -1, 1},
                new double[]{1, -1, 1, 1},
                new double[]{-1, 1, 1, 1},
        };

        //should be 4
        double[][] determinantTestData = new double[][] {
                new double[]{1, -1, 1},
                new double[]{1, 1, 1},
                new double[]{-1, 1, 1},
        };

        double[][] cofactorTestData = new double[][] {
                new double[]{0,  1,  2,  3},
                new double[]{4,  5,  6,  7},
                new double[]{8,  9,  10, 11},
                new double[]{12, 13, 14, 15},
        };

        Matrix m =
                projection;
//                new Matrix(inverseTestData);

        long start = System.nanoTime();
        double[][] inverse = m.inverse_4x4();
        long end = System.nanoTime();

        Matrix m_inverse = new Matrix(inverse);

        double[] point = new double[]{5, 5, 5};

        double[] out1 = MatrixHelper.applyImplicitMatrix(m, point);
        double[] out2 = MatrixHelper.applyImplicitMatrix(m_inverse, out1);

        System.out.println(String.format("Point %s applied with matrix is %s", Arrays.toString(point), Arrays.toString(out1)));
        System.out.println(String.format("Output %s applied with inverse is %s", Arrays.toString(out1), Arrays.toString(out2)));
        System.out.println(String.format("Inverting a matrix takes %s ns", end-start));
    }
}
