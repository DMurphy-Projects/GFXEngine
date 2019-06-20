package TextureGraphics.Memory;

import org.jocl.*;

import java.nio.ByteBuffer;

import static org.jocl.CL.clReleaseMemObject;

public abstract class BaseJoclMemory implements IJoclMemory {

    cl_mem memoryObject;

    @Override
    public abstract void create(cl_context context, cl_command_queue commandQueue, ByteBuffer buffer, long type);

    @Override
    public abstract void write(cl_command_queue commandQueue, ByteBuffer buffer, int offset);

    public void create(cl_context context, int size, long type)
    {
        memoryObject = JoclMemoryMethods.createEmpty(context, size, type);
    }

    @Override
    public void release() {
        clReleaseMemObject(memoryObject);
    }

    @Override
    public Pointer getObject() {
        return Pointer.to(memoryObject);
    }

    @Override
    public cl_mem getRawObject() {
        return memoryObject;
    }
}
