package TextureGraphics.Memory;

import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_event;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class MemoryHandler {

    private String prefix = "UNNAMED_";
    private int count = 0;

    HashMap<String, Integer> names = new HashMap<>();
    ArrayList<IJoclMemory> memory = new ArrayList<>();

    cl_context context;
    cl_command_queue commandQueue;

    public MemoryHandler(cl_context context, cl_command_queue commandQueue)
    {
        this.context = context;
        this.commandQueue = commandQueue;
    }

    //async controls the type of handling this empty buffer will have
    public IJoclMemory put(String name, int totalSize, long type, boolean sync)
    {
        return putEmpty(name, totalSize, type, sync);
    }
    //----------Start of primitive methods

    public IJoclMemory put(cl_event task, String name, double[] arr, int offset, long type)
    {
        //TODO is it worse to use sync write with a direct buffer
        ByteBuffer buffer = BufferHelper.createBuffer(arr);

        return put(task, name, buffer, offset, type);
    }

    public IJoclMemory put(cl_event task, String name, int[] arr, int offset, long type)
    {
        ByteBuffer buffer = BufferHelper.createBuffer(arr);

        return put(task, name, buffer, offset, type);
    }

    //----------End of primitive methods

    //entry point for primitive methods, switches between asyc, sync and non-writing implementations
    //Switches:
    //task present => async
    //name missing => assign name
    //name already exists => write into existing
    private IJoclMemory put(cl_event task, String name, ByteBuffer buffer, int offset, long type)
    {
        if (name == null) {
            name = prefix + count++;
        }

        if (task == null)
        {
            return putSync(name, buffer, offset, type);
        }
        else
        {
            return putAsync(task, name, buffer, offset, type);
        }
    }

    //----------Async Start
    protected IJoclMemory putAsync(cl_event task, String name, ByteBuffer buffer, int offset, long type)
    {
        if (addName(name))
        {
            IJoclMemory m = JoclMemoryFacade.createAsync(context, commandQueue, task, buffer, type);
            memory.add(m);
            return m;
        }
        else
        {
            return putAsync(name, buffer, offset);
        }
    }

    protected IJoclMemory putAsync(String name, ByteBuffer buffer, int offset)
    {
        IJoclMemory m = get(name);

        m.write(commandQueue, buffer, offset);

        return m;
    }
    //----------Async End

    //----------Sync Start
    protected IJoclMemory putSync(String name, ByteBuffer buffer, int offset, long type)
    {
        if (addName(name)) {
            IJoclMemory m = JoclMemoryFacade.createBlocking(context, commandQueue, buffer, type);
            memory.add(m);
            return m;
        }
        else
        {
            return putSync(name, buffer, offset);
        }
    }

    protected IJoclMemory putSync(String name, ByteBuffer buffer, int offset)
    {
        IJoclMemory m = get(name);

        m.write(commandQueue, buffer, offset);

        return m;
    }
    //----------Sync End

    //----------Create Empty
    protected IJoclMemory putEmpty(String name, int totalSize, long type, boolean sync)
    {
        //if name already exists, remove it
        if (!addName(name))
        {
            remove(name);
            addName(name);
        }

        IJoclMemory m = JoclMemoryFacade.createEmpty(context, totalSize, type, sync);
        memory.add(m);
        return m;
    }

    private boolean addName(String name)
    {
        if (!nameTaken(name))
        {
            names.put(name, memory.size());
            return true;
        }
        else
        {
            return false;
        }
    }

    private boolean nameTaken(String name)
    {
        return names.containsKey(name);
    }

    public IJoclMemory get(String name)
    {
        return memory.get(names.get(name));
    }

    private void remove(String name)
    {
        memory.get(names.get(name)).release();
        names.remove(name);
    }

    public void releaseAll()
    {
        for (IJoclMemory m:memory)
        {
            m.release();
        }
    }
}
