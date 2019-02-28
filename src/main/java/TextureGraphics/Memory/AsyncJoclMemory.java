package TextureGraphics.Memory;

import org.jocl.*;

import java.nio.ByteBuffer;

import static org.jocl.CL.clReleaseMemObject;
import static org.jocl.CL.clWaitForEvents;

public class AsyncJoclMemory extends JoclMemory {

    cl_event finishedWriting, taskFinished;
    cl_mem memoryObject;

    public void create(cl_context context, cl_command_queue commandQueue, cl_event tF, ByteBuffer buffer, long type)
    {
        taskFinished = tF;
        finishedWriting = new cl_event();

        memoryObject = JoclMemoryMethods.asyncWrite(context, commandQueue, buffer, finishedWriting, type);
    }

    @Override
    public void release() {
        if (taskFinished != null) {
            clWaitForEvents(1, new cl_event[]{taskFinished});
        }
        clReleaseMemObject(memoryObject);
        memoryObject = null;
    }

    public cl_event getFinishedWritingEvent()
    {
        return finishedWriting;
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
