package GxEngine3D.Helper;

import GxEngine3D.Model.Matrix.Matrix;

public class FrustumMatrixHelper {
    private static double[] gluPerspective(double angleOfView, double imageAspectRatio, double n, double f, double zoom)
    {
        double scale = (Math.tan(angleOfView * 0.5 * Math.PI / 180) * n) / zoom;
        double r = imageAspectRatio * scale;
        double l = -r;
        double t = scale;
        double b = -t;
        return new double[]{b, t, l, r};
    }

    private static Matrix glFrustum(double b, double t, double l, double r,
                             double n, double f)
    {
        double[][] m = new double[4][4];
        m[0][0] = 2 * n / (r - l);

        m[1][1] = 2 * n / (t - b);

        m[0][2] = (r + l) / (r - l);
        m[1][2] = (t + b) / (t - b);
        m[2][2] = -(f + n) / (f - n);
        m[3][2] = -1;

        m[2][3] = -2 * f * n / (f - n);

        return new Matrix(m);
    }

    //aOV - angle of view, ie 90 degrees, etc
    //near - near clipping plane
    //far - far clipping plane
    //height/width - size of the screen/component used
    //zoom - high number zoom in, low number zoom out
    public static Matrix createMatrix(double aOV, double near, double far, double width, double height, double zoom)
    {
        double imageAspectRatio = width / height;

        double[] btlr = gluPerspective(aOV, imageAspectRatio, near, far, zoom);
        return glFrustum(btlr[0], btlr[1], btlr[2], btlr[3], near, far);
    }
}
