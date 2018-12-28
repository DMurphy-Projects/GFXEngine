package TextureGraphics.Memory;

import org.jocl.Pointer;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_mem;

import static org.jocl.CL.clReleaseMemObject;

public class SyncJoclMemory extends JoclMemory{

    cl_mem memoryObject;

    public void create(cl_context context, cl_command_queue commandQueue, double[] arr, long type) {
        memoryObject = JoclMemoryMethods.write(context, commandQueue, arr, type);
    }

    public void create(cl_context context, cl_command_queue commandQueue, int size, long type)
    {
        memoryObject = JoclMemoryMethods.createEmpty(context, commandQueue, size, type);
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
