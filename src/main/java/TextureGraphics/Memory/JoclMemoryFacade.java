package TextureGraphics.Memory;

import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_event;

import java.nio.ByteBuffer;

public class JoclMemoryFacade {
    public static IJoclMemory createAsync(cl_context context, cl_command_queue commandQueue, cl_event tF, ByteBuffer buffer, long type)
    {
        AsyncJoclMemory m = new AsyncJoclMemory(tF);
        m.create(context, commandQueue, buffer, type);
        return m;
    }

    public static IJoclMemory createBlocking(cl_context context, cl_command_queue commandQueue, ByteBuffer buffer, long type)
    {
        SyncJoclMemory m = new SyncJoclMemory();
        m.create(context, commandQueue, buffer, type);
        return m;
    }

    public static IJoclMemory createEmpty(cl_context context, int size, long type, cl_event taskEvent)
    {
        IJoclMemory m;
        if (taskEvent == null)
        {
            m = new SyncJoclMemory();
        }
        else
        {
            m = new AsyncJoclMemory(taskEvent);
        }

        m.create(context, size, type);
        return m;
    }
}
