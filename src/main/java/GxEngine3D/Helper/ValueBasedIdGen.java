package GxEngine3D.Helper;

public class ValueBasedIdGen {

    public static String generate(double[] values)
    {
        String id = "";
        for (int i=0;i<values.length-1;i++)
        {
            id += generate(values[i]) + "_";
        }
        id += generate(values[values.length-1]);
        return id;
    }

    public static String generate(double value)
    {
        return Double.toString(value);
    }
}
