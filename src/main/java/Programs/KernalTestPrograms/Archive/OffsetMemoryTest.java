package Programs.KernalTestPrograms.Archive;

import TextureGraphics.JoclProgram;
import TextureGraphics.Memory.AsyncJoclMemory;
import TextureGraphics.Memory.BufferHelper;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_event;
import org.jocl.cl_mem;

import java.nio.ByteBuffer;

import static org.jocl.CL.*;

public class OffsetMemoryTest extends JoclProgram {

    public static void main(String[] args)
    {
        OffsetMemoryTest test = new OffsetMemoryTest();
        test.setup();
    }

    public int[] createArray(int size, int fill)
    {
        int[] arr = new int[size];
        for (int i=0;i<size;i++)
        {
            arr[i] = fill;
        }
        return arr;
    }

    public OffsetMemoryTest()
    {
        //we only need a context for the test, the kernel will not be executed
        create("resources/Kernels/TextureMethods/TextureRenderPolyBary.cl", "drawTriangle");

        super.start();
    }

    @Override
    public void setup() {
        super.setup();

        test02();
    }

    private void test02()
    {
        int[] data1 = createArray(10, 1);
        int[] data2 = createArray(10, 2);
        int dataSize = data1.length + data2.length;

        cl_event task = new cl_event();

        //manually create a buffer with extra room
        AsyncJoclMemory m = (AsyncJoclMemory) dynamic.put(task, "TestArray", dataSize * Sizeof.cl_int, CL_MEM_READ_WRITE);

        //write both arrays into the same memory object
        dynamic.put(task, "TestArray", data1, 0, 0);
        dynamic.put(task, "TestArray", data2, data1.length * Sizeof.cl_int, 0);

        cl_event[] writingEvents = m.getFinishedWritingEvent();

        clWaitForEvents(writingEvents.length, writingEvents);

        int[] out = new int[dataSize];
        //read data
        clEnqueueReadBuffer(commandQueue, dynamic.get("TestArray").getRawObject(), true, 0,
                Sizeof.cl_int * dataSize, Pointer.to(out), 0, null, null);

        for (int i:out)
        {
            System.out.println(i);
        }
    }

    private void test01()
    {
        int[] data1 = createArray(10, 1);
        int[] data2 = createArray(10, 2);
        int offset = 0;
        int dataSize = data1.length + data2.length;

        ByteBuffer b1 = BufferHelper.createBuffer(data1);
        ByteBuffer b2 = BufferHelper.createBuffer(data2);
        int bufferSize = b1.capacity() + b2.capacity();

        cl_mem memoryObject = clCreateBuffer(context, CL_MEM_READ_WRITE,
                bufferSize, null, null);

        cl_event[] events = new cl_event[2];

        //write first half
        events[0] = new cl_event();
        clEnqueueWriteBuffer(commandQueue, memoryObject, false, offset,
                b1.capacity(), Pointer.to(b1), 0, null, events[0]);
        offset += b1.capacity();

        //write second half
        events[1] = new cl_event();
        clEnqueueWriteBuffer(commandQueue, memoryObject, false, offset,
                b2.capacity(), Pointer.to(b2), 0, null, events[1]);

        clWaitForEvents(events.length, events);

        int[] out = new int[dataSize];
        //read data
        clEnqueueReadBuffer(commandQueue, memoryObject, true, 0,
                Sizeof.cl_uint * dataSize, Pointer.to(out), 0, null, null);

        for (int i:out)
        {
            System.out.println(i);
        }
    }
}
