package TextureGraphics.Memory;

import oracle.jrockit.jfr.events.Bits;
import org.jocl.Sizeof;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BufferHelper {

    private static ByteOrder order = ByteOrder.nativeOrder();

    public static ByteBuffer createBuffer(double[] arr)
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(arr.length * Sizeof.cl_double);
        buffer.order(order);

        for (double e:arr)
        {
            buffer.putDouble(e);
        }
        buffer.rewind();

        return buffer;
    }

    public static ByteBuffer createBuffer(int[] arr)
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(arr.length * Sizeof.cl_int);
        buffer.order(order);

        for (int e:arr)
        {
            buffer.putInt(e);
        }
        buffer.rewind();

        return buffer;
    }
}
