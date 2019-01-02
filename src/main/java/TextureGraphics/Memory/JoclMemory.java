package TextureGraphics.Memory;

import org.jocl.*;

public abstract class JoclMemory {

    public static JoclMemory createAsync(cl_context context, cl_command_queue commandQueue, cl_event tF, double[] arr, long type)
    {
        AsyncJoclMemory m = new AsyncJoclMemory();
        m.create(context, commandQueue, tF, arr, type);
        return m;
    }

    public static JoclMemory createBlocking(cl_context context, cl_command_queue commandQueue, double[] arr, long type)
    {
        SyncJoclMemory m = new SyncJoclMemory();
        m.create(context, commandQueue, arr, type);
        return m;
    }

    public static JoclMemory createEmpty(cl_context context, cl_command_queue commandQueue, int size, long type)
    {
        SyncJoclMemory m = new SyncJoclMemory();
        m.create(context, commandQueue, size, type);
        return m;
    }

    public abstract void release();
    public abstract Pointer getObject();
    public abstract cl_mem getRawObject();

}
