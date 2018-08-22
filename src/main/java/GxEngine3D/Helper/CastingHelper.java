package GxEngine3D.Helper;

public class CastingHelper {
    public static double[] convert(Double[] d)
    {
        double _d[] = new double[d.length];
        for (int i=0;i<d.length;i++)
        {
            _d[i] = d[i];
        }
        return _d;
    }
}
