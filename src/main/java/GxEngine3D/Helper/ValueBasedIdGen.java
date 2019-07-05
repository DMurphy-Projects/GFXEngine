package GxEngine3D.Helper;

import java.nio.ByteBuffer;

public class ValueBasedIdGen {

    public static <T extends Number> String generate(T[] values)
    {
        String id = "";
        for (int i=0;i<values.length-1;i++)
        {
            id += generate(values[i]) + "_";
        }
        id += generate(values[values.length-1]);
        return id;
    }

    public static <T extends Number> String generate(T value)
    {
        return value.toString();
    }

    public static String generate(ByteBuffer buffer)
    {
        String id = "";

        buffer.rewind();
        while(buffer.hasRemaining())
        {
            id += generate(buffer.get()) + "_";
        }

        return id;
    }
}
