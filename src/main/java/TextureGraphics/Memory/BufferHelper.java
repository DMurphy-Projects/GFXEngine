package TextureGraphics.Memory;

import oracle.jrockit.jfr.events.Bits;
import org.jocl.Sizeof;

import java.nio.ByteBuffer;

public class BufferHelper {

    public static ByteBuffer createBuffer(double[] arr)
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(arr.length * Sizeof.cl_double);
        for (double e:arr)
        {
            buffer.putDouble(Bits.swap(e));
        }
        buffer.rewind();

        return buffer;
    }

    public static ByteBuffer createBuffer(int[] arr)
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(arr.length * Sizeof.cl_int);
        for (int e:arr)
        {
            buffer.putInt(Bits.swap(e));
        }
        buffer.rewind();

        return buffer;
    }
}
