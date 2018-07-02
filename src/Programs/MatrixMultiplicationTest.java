package Programs;

import DebugTools.TextModule.TextWhitelist;
import DebugTools.TextOutput;
import GxEngine3D.Helper.MatrixHelper;
import GxEngine3D.Model.Matrix.Matrix;

import java.util.Arrays;

public class MatrixMultiplicationTest {
    public static void main(String[] args)
    {
        TextOutput.setModule(new TextWhitelist(Matrix.class.getName()));
        double[][] d1 = new double[][] {
            {1, 2, 3},
            {4, 5, 6},
        };
        double[][] d2 = new double[][] {
                {7, 8},
                {9, 10},
                {11, 12},
        };
        Matrix m1 = new Matrix(d1);
        Matrix m2 = new Matrix(d2);

        m1 = new Matrix(m1.matrixMultiply(m2));
        System.out.println(m1);

        double[][] t = MatrixHelper.setupTranslateMatrix(5, 5, 5);
        double[][] r = MatrixHelper.setupYawRotation(1);
        double[][] s = MatrixHelper.setupIdentityMatrix();

        double[] point = new double[]{1, 1, 1};

        m1 = new Matrix(s);
        m2 = new Matrix(r);
        Matrix m3 = new Matrix(t);

        double[] p1 = m1.pointMultiply(point);
//        p1 = m2.pointMultiply(p1);
        p1 = m3.pointMultiply(p1);

        System.out.println(Arrays.toString(p1));

//        m1 = new Matrix(m1.matrixMultiply(m2));
        m1 = new Matrix(m1.matrixMultiply(m3));

        p1 = m1.pointMultiply(point);

        System.out.println(Arrays.toString(p1));

    }
}
