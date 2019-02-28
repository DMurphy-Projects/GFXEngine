package TextureGraphics.Memory;

import GxEngine3D.Helper.PerformanceTimer;
import oracle.jrockit.jfr.events.Bits;
import org.jocl.*;

import java.nio.ByteBuffer;

import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clEnqueueWriteBuffer;

public class JoclMemoryMethods {

    public static cl_mem asyncWrite(cl_context context, cl_command_queue commandQueue, ByteBuffer buffer, cl_event finishedWriting, long bufferType) {
        //this is approx. 10% of this method
        cl_mem  memoryObject = clCreateBuffer(context, bufferType,
                buffer.capacity(), null, null);

        return asyncWrite(commandQueue, buffer, finishedWriting, memoryObject);
    }

    public static cl_mem asyncWrite(cl_command_queue commandQueue, ByteBuffer byteBuffer, cl_event finishedWriting, cl_mem memoryObject)
    {
        //this is approx. 85% of this method
        clEnqueueWriteBuffer(commandQueue, memoryObject, false, 0,
                byteBuffer.capacity(), Pointer.to(byteBuffer), 0, null, finishedWriting);

        return memoryObject;
    }

    public static cl_mem write(cl_context context, cl_command_queue commandQueue, ByteBuffer buffer, long bufferType)
    {
        cl_mem  memoryObject = clCreateBuffer(context, bufferType,
                buffer.capacity(), null, null);
        clEnqueueWriteBuffer(commandQueue, memoryObject, true, 0,
                buffer.capacity(), Pointer.to(buffer), 0, null, null);
        return memoryObject;
    }

    public static cl_mem createEmpty(cl_context context, cl_command_queue commandQueue, int size, long bufferType)
    {
        cl_mem  memoryObject = clCreateBuffer(context, bufferType,
                size, null, null);
        return memoryObject;
    }
}
