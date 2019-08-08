package TextureGraphics.Memory.Array;
import TextureGraphics.JoclSetup;
import org.jocl.*;

import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.jocl.CL.clCreateBuffer;

public class ArrayDataHandler {

    cl_context context;
    cl_command_queue commandQueue;

    HashMap<Object, ArrayData> partData = new HashMap<>();
    cl_mem dataArray;
    int memoryOffset;

    Queue<Object> toUpdate = new ConcurrentLinkedQueue<>();

    public ArrayDataHandler(JoclSetup setup)
    {
        context = setup.getContext();
        commandQueue = setup.getCommandQueue();
    }

    public void init(int dataSize, int dataType, long memoryFlag)
    {
        dataArray = clCreateBuffer(context, memoryFlag,dataSize * dataType, null, null);

        memoryOffset = 0;
    }

    public void create(ArrayData data, Object belongsTo)
    {
        partData.put(belongsTo, data);

        data.setOffset(memoryOffset);
        memoryOffset += data.byteSize;

        toUpdate.add(belongsTo);
    }

    public ArrayData retrieveLocalData(Object belongsTo, boolean update)
    {
        if (partData.containsKey(belongsTo))
        {
            if (update && !toUpdate.contains(belongsTo))
            {
                toUpdate.add(belongsTo);
            }

            return partData.get(belongsTo);
        }

        System.out.println("Object: "+ belongsTo + " had no data to return");
        return null;
    }

    public cl_mem retrieveDeviceData()
    {
        update();

        return dataArray;
    }

    public void update()
    {
        Object o;
        while((o = toUpdate.poll()) != null)
        {
            update(o);
        }
    }

    public void update(Object belongsTo)
    {
        ArrayData data = partData.get(belongsTo);

        data.writeToQueue(commandQueue, dataArray);
    }

    public int getMemoryOffset()
    {
        return memoryOffset;
    }
}
