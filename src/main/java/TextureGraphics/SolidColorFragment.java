package TextureGraphics;

import TextureGraphics.Memory.AsyncJoclMemory;
import TextureGraphics.Memory.BufferHelper;
import TextureGraphics.Memory.IJoclMemory;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_event;
import org.jocl.cl_mem;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import static org.jocl.CL.*;

//this implementation is based on the GpuRendererIterateColorPolygon
//the handling of the polygon points will be external
//
public class SolidColorFragment extends  JoclProgram{

    int screenWidth, screenHeight;

    //names to retrieve arguments by
    String outMap = "OutMap", screenSize = "ScreenSize";

    //arg indices
    int screenSizeArg = 0,
            outMapArg = 1,
            metaInfoArg = 2,
            colorArrayArg = 3,
            outPixelArg = 4,
            indexArrayArg = 5
                    ;

    ArrayList<cl_event> taskEvents;
    cl_event taskEvent;

    int[] colorArray;

    boolean colorArrayUpdated = false;

    public SolidColorFragment(int screenWidth, int screenHeight, JoclSetup setup)
    {
        this.profiling = setup.isProfiling();

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create("resources/Kernels/FragmentRender/SolidColorFragment.cl", "render",
                setup);

        super.start();
    }

    @Override
    protected void initStaticMemory() {
        super.initStaticMemory();

        immutable.put(null, screenSize, new int[]{screenWidth, screenHeight}, 0, CL_MEM_READ_ONLY);
        setupScreenSizeArgs();
    }

    @Override
    public void setup() {
        super.setup();
        taskEvents = new ArrayList<>();

        taskEvent = new cl_event();
    }

    public void prepareColorArray(int size, int[] colorArray)
    {
        this.colorArray = new int[size];

        for (int i=0;i<size;i++)
        {
            this.colorArray[i] = colorArray[i % colorArray.length];
        }

        colorArrayUpdated = true;
    }

    public void enqueueTasks(cl_event externalTask)
    {
        int size = 1;
        if (colorArrayUpdated) size+= 1;
        //write all data to device
        cl_event[] writingEvents = new cl_event[size];

        int i = 0;
        writingEvents[i++] = externalTask;

        if (colorArrayUpdated)
        {
            writingEvents[i++] = setupColorArray(taskEvent);
            colorArrayUpdated = false;
        }

        //enqueue ranges
        long[] globalWorkSize = new long[] {
                (long) screenWidth,
                (long) screenHeight
        };

        //TODO needs to be a relationship with the screen dimensions, currently hard coded
        long[] localWorkSize = new long[] {
                32, 32
        };

        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null,
                globalWorkSize, localWorkSize, writingEvents.length, writingEvents, taskEvent);

        taskEvents.add(taskEvent);
    }

    public boolean waitOnFinishing()
    {
        if (taskEvents.size() > 0) {
            cl_event[] executionTasks = new cl_event[taskEvents.size()];
            taskEvents.toArray(executionTasks);

            clWaitForEvents(executionTasks.length, executionTasks);
            return true;
        }
        return false;
    }

    public void finish()
    {
        super.finish();
    }

    private void setupScreenSizeArgs()
    {
        clSetKernelArg(kernel, screenSizeArg, Sizeof.cl_mem, immutable.get(screenSize).getObject());
    }

    //----------EXTERNAL
    public void setupOutMap(IJoclMemory m)
    {
        clSetKernelArg(kernel, outMapArg, Sizeof.cl_mem, m.getObject());
    }
    public void setupIndexArray(IJoclMemory m)
    {
        clSetKernelArg(kernel, indexArrayArg, Sizeof.cl_mem, m.getObject());
    }
    public void setupMetaInfoArray(Pointer pointer)
    {
        clSetKernelArg(kernel, metaInfoArg, Sizeof.cl_mem, pointer);
    }
    public void setupPixelOut(Pointer pointer)
    {
        clSetKernelArg(kernel, outPixelArg, Sizeof.cl_mem, pointer);
    }
    //----------EXTERNAL


    private cl_event setupColorArray(cl_event task)
    {
        IJoclMemory m = cached.put(task, null, colorArray, 0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, colorArrayArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }
}
