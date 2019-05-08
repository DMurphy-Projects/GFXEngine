package GxEngine3D.Helper.Maths;

//TODO is this needed anymore?
public class Vector2DCalc {
    public static double side(double[] v0, double[] v1, double[] p)
    {
        return (v1[0] - v0[0])*(p[1] - v0[1]) - (v1[1] - v0[1])*(p[0] - v0[0]);
    }
}
