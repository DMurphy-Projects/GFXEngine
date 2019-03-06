package GxEngine3D.Helper;

public class ArrayHelper {
    public static double[] flatten(double[][] data)
    {
        int m = data.length, n = data[0].length;
        double[] flat = new double[m * n];
        int i = 0;

        for (int _m = 0;_m < m;_m++)
        {
            for (int _n = 0;_n<n;_n++)
            {
                flat[i] = data[_m][_n];
                i++;
            }
        }
        return flat;
    }
}
