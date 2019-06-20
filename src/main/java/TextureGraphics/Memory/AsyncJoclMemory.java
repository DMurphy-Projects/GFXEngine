package TextureGraphics.Memory;

import org.jocl.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.jocl.CL.clReleaseMemObject;
import static org.jocl.CL.clWaitForEvents;

public class AsyncJoclMemory extends BaseJoclMemory {

    ArrayList<cl_event> finishedWriting;
    cl_event taskFinished;

    public AsyncJoclMemory(cl_event taskFinished)
    {
        finishedWriting = new ArrayList<>();

        this.taskFinished = taskFinished;
    }

    public void create(cl_context context, cl_command_queue commandQueue, ByteBuffer buffer, long type)
    {
        //if we're recreating then the previous events don't matter anymore
        finishedWriting.clear();

        cl_event writing = new cl_event();
        finishedWriting.add(writing);

        //this will also create the memory object
        memoryObject = JoclMemoryMethods.asyncWrite(context, commandQueue, buffer, writing, type);
    }

    public void write(cl_command_queue commandQueue, ByteBuffer buffer, int offset)
    {
        cl_event writing = new cl_event();
        finishedWriting.add(writing);

        //writes to the existing memory object
        JoclMemoryMethods.asyncWrite(commandQueue, buffer, offset, writing, memoryObject);
    }

    @Override
    public void release() {
        if (taskFinished != null) {
            clWaitForEvents(1, new cl_event[]{taskFinished});
        }

        super.release();
    }

    public cl_event[] getFinishedWritingEvent()
    {
        cl_event[] events = new cl_event[finishedWriting.size()];
        finishedWriting.toArray(events);
        return events;
    }
}
