package TextureGraphics.Memory;

import oracle.jrockit.jfr.events.Bits;
import org.jocl.*;

import java.nio.ByteBuffer;

import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clEnqueueWriteBuffer;

public class JoclMemoryMethods {
    public static cl_mem asyncWrite(cl_context context, cl_command_queue commandQueue, double[] arr, cl_event finishedWriting, long bufferType)
    {
        cl_mem  memoryObject = clCreateBuffer(context, bufferType,
                arr.length * Sizeof.cl_double, null, null);

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(arr.length * Sizeof.cl_double);

        for (double d:arr)
        {
            byteBuffer.putDouble(Bits.swap(d));
        }
        byteBuffer.rewind();

        clEnqueueWriteBuffer(commandQueue, memoryObject, false, 0,
                arr.length * Sizeof.cl_double, Pointer.to(byteBuffer), 0, null, finishedWriting);
        return memoryObject;
    }

    public static cl_mem write(cl_context context, cl_command_queue commandQueue, double[] arr, long bufferType)
    {
        cl_mem  memoryObject = clCreateBuffer(context, bufferType,
                arr.length * Sizeof.cl_double, null, null);
        clEnqueueWriteBuffer(commandQueue, memoryObject, true, 0,
                arr.length * Sizeof.cl_double, Pointer.to(arr), 0, null, null);
        return memoryObject;
    }

    public static cl_mem createEmpty(cl_context context, cl_command_queue commandQueue, int size, long bufferType)
    {
        cl_mem  memoryObject = clCreateBuffer(context, bufferType,
                size, null, null);
        return memoryObject;
    }
}
