package TextureGraphics.Memory;

import oracle.jrockit.jfr.events.Bits;
import org.jocl.Sizeof;
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
    ArrayList<JoclMemory> memory = new ArrayList<>();

    cl_context context;
    cl_command_queue commandQueue;

    public MemoryHandler(cl_context context, cl_command_queue commandQueue)
    {
        this.context = context;
        this.commandQueue = commandQueue;
    }

    public JoclMemory put(String name, int totalSize, long type)
    {
        return put(name, totalSize, type, nameValid(name));
    }

    //----------Start of primitive methods

    public JoclMemory put(cl_event task, String name, double[] arr, long type)
    {
        //TODO is it worse to use sync write with a direct buffer
        ByteBuffer buffer = BufferHelper.createBuffer(arr);

        return put(task, name, buffer, type);
    }

    public JoclMemory put(cl_event task, String name, int[] arr, long type)
    {
        ByteBuffer buffer = BufferHelper.createBuffer(arr);

        return put(task, name, buffer, type);
    }

    //----------End of primitive methods

    //entry point for primitive methods, switches between asyc, sync and non-writing implementations
    private JoclMemory put(cl_event task, String name, ByteBuffer buffer, long type)
    {
        boolean valid;
        if (name == null) {
            name = prefix + count++;
            valid = true;
        }
        else
        {
            valid = nameValid(name);

        }

        if (task == null)
        {
            return put(task, name, buffer, type, valid);
        }
        else
        {
            return put(task, name, buffer, type, valid);
        }
    }

    protected JoclMemory put(cl_event task, String name, ByteBuffer buffer, long type, boolean nameValid)
    {
        nameCheck(name, nameValid);

        JoclMemory m = JoclMemory.createAsync(context, commandQueue, task, buffer, type);
        memory.add(m);
        return m;
    }

    protected JoclMemory put(String name, ByteBuffer buffer, long type, boolean nameValid)
    {
        nameCheck(name, nameValid);

        JoclMemory m = JoclMemory.createBlocking(context, commandQueue, buffer, type);
        memory.add(m);
        return m;
    }

    protected JoclMemory put(String name, int totalSize, long type, boolean nameValid)
    {
        nameCheck(name, nameValid);

        JoclMemory m = JoclMemory.createEmpty(context, commandQueue, totalSize, type);
        memory.add(m);
        return m;
    }

    private void nameCheck(String name, boolean nameValid)
    {
        if (!nameValid)
        {
            System.out.println(String.format("Name '%s' already exists, releasing previous object ", name));
            memory.get(names.get(name)).release();
        }
        names.put(name, memory.size());
    }

    //if there is no key, then we can use it
    private boolean nameValid(String name)
    {
        return !names.containsKey(name);
    }

    public JoclMemory get(String name)
    {
        return memory.get(names.get(name));
    }

    public void releaseAll()
    {
        for (JoclMemory m:memory)
        {
            m.release();
        }
    }
}
