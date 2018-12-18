package GxEngine3D.Helper;

import GxEngine3D.Model.Matrix.Matrix;

//this is make using 4*4 matrices easier by providing initialising code for various functions
public class MatrixHelper {

    public static double[][] setupFullRotation(double pitch, double yaw, double roll)
    {
        double[][] d;
        Matrix rotation = new Matrix(MatrixHelper.setupPitchRotation(0));

        if (pitch != 0) {
            d = MatrixHelper.setupPitchRotation(pitch);
            rotation = new Matrix(rotation.matrixMultiply(d));
        }

        if (yaw != 0) {
            d = MatrixHelper.setupYawRotation(yaw);
            rotation = new Matrix(rotation.matrixMultiply(d));
        }

        if (roll != 0) {
            d = MatrixHelper.setupRollRotation(roll);
            rotation = new Matrix(rotation.matrixMultiply(d));
        }
        return rotation.getMatrix();
    }

    public static double[][] setupPitchRotation(double pitch)
    {
        double[][] pitchRot = new double[4][4];
        double cosPitch = Math.cos(pitch), sinPitch = Math.sin(pitch);
        pitchRot[0][0] = 1;
        pitchRot[1][1] = cosPitch;
        pitchRot[2][2] = cosPitch;
        pitchRot[3][3] = 1;
        pitchRot[2][1] = -sinPitch;
        pitchRot[1][2] = sinPitch;
        return pitchRot;
    }

    public static double[][] setupYawRotation(double yaw)
    {
        double[][] yawRot = new double[4][4];
        double cosYaw = Math.cos(yaw), sinYaw = Math.sin(yaw);
        yawRot[0][0] = cosYaw;
        yawRot[1][1] = 1;
        yawRot[2][2] = cosYaw;
        yawRot[3][3] = 1;
        yawRot[0][2] = sinYaw;
        yawRot[2][0] = -sinYaw;
        return yawRot;
    }

    public static double[][] setupRollRotation(double roll)
    {
        double[][] rollRot = new double[4][4];
        double cosRoll = Math.cos(roll), sinRoll = Math.sin(roll);
        rollRot[0][0] = cosRoll;
        rollRot[1][1] = cosRoll;
        rollRot[2][2] = 1;
        rollRot[3][3] = 1;
        rollRot[0][1] = -sinRoll;
        rollRot[1][0] = sinRoll;
        return rollRot;
    }

    public static double[][] setupIdentityMatrix()
    {
        double[][] identity = new double[4][4];
        identity[0][0] = 1;
        identity[1][1] = 1;
        identity[2][2] = 1;
        identity[3][3] = 1;
        return identity;
    }

    public static double[][] setupTranslateMatrix(double[] translate)
    {
        return setupTranslateMatrix(translate[0], translate[1], translate[2]);
    }

    public static double[][] setupTranslateMatrix(double x, double y, double z)
    {
        double[][] translate = new double[4][4];
        translate[0][3] = x;
        translate[1][3] = y;
        translate[2][3] = z;

        translate[0][0] = 1;
        translate[1][1] = 1;
        translate[2][2] = 1;
        translate[3][3] = 1;
        return translate;
    }

    public static double[] applyImplicitMatrix(Matrix m, double[] d)
    {
        d = m.pointMultiply(d);
        return new double[]{
                d[0] /= d[3],
                d[1] /= d[3],
                d[2] /= d[3]
        };
    }

    //explicit should be used for the projection matrix
    public static double[] applyExplicitMatrix(Matrix m, double[] d)
    {
        d = m.pointMultiply(d);
        double absP = Math.abs(d[3]);
        return new double[]{
                d[0] / absP,
                d[1] / absP,
                d[2] / absP,
        };
    }
}
