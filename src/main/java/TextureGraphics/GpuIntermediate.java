package TextureGraphics;

import TextureGraphics.Memory.AsyncJoclMemory;
import TextureGraphics.Memory.BufferHelper;
import TextureGraphics.Memory.IJoclMemory;
import org.jocl.*;

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
public class GpuIntermediate extends  JoclProgram{

    int screenWidth, screenHeight;

    //names to retrieve arguments by
    String outMap = "OutMap", zMapOut = "ZMap", screenSize = "ScreenSize", indexArray = "IndexArray";

    //arg indices
    int nArg = 0,
            screenSizeArg = 1,
            polygonArrayArg = 2,
            polygonStartArrayArg = 3,
            boundBoxArrayArg = 4,
            outArg = 5,
            zMapArg = 6,
            indexArrayArg = 7
                    ;

    //data arrays
    int[] boundBoxArray, indexArrayData;
    ByteBuffer zMapBuffer, outMapBuffer;

    int size;

    ArrayList<cl_event> taskEvents;
    cl_event taskEvent;

    public GpuIntermediate(int screenWidth, int screenHeight, JoclSetup setup)
    {
        this.profiling = setup.isProfiling();

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create("resources/Kernels/IterateMethods/IterateIntermediate.cl", "calculateMapInfo",
                setup);

        super.start();

        double[] zMapStart = new double[screenWidth*screenHeight];
        Arrays.fill(zMapStart, 1);

        zMapBuffer = BufferHelper.createBuffer(zMapStart);

        int[] pixelStart = new int[screenWidth*screenHeight*2];//we need 2 integers per pixel, polygon position and local triangle position
        outMapBuffer = BufferHelper.createBuffer(pixelStart);
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

        setupOutputMemory();
    }

    public cl_event getTask() {
        return taskEvent;
    }

    public IJoclMemory getIndexArray()
    {
        return dynamic.get(indexArray);
    }

    public IJoclMemory getOutMap()
    {
        return dynamic.get(outMap);
    }

    public int[] getIndexArrayData()
    {
        return indexArrayData;
    }

    public void prepare(IntBuffer indexArrayData)
    {
        this.size = 0;
        int index = 0;

        ArrayList<Integer> indexArrayList = new ArrayList<>();

        while(indexArrayData.hasRemaining())
        {
            int i = indexArrayData.get();
            if (i == 0)
            {
                indexArrayList.add(index);

                this.size++;
            }

            index++;
        }

        if (size == 0) return;

        boundBoxArray = new int[size*4];//each element contains 4 integers, [x, y, width, height]
        this.indexArrayData = new int[size];

        int i = 0;
        for (Integer _i: indexArrayList)
        {
            this.indexArrayData[i] = _i;
            i++;
        }
    }

    public void setupBoundBox(DoubleBuffer screenPolygonData, int[] polygonStart)
    {
        int polygonCount = 0;

        for (int i: indexArrayData) {
            int start = polygonStart[i];
            int end = polygonStart[i + 1];
            int size = (end - start) / 3;

            Polygon p = new Polygon();

            for (int _i = 0; _i < size; _i++) {
                screenPolygonData.position(start + (_i * 3));

                p.addPoint((int) screenPolygonData.get(), (int) screenPolygonData.get());
            }

            Rectangle r = p.getBounds();
            boundBoxArray[(polygonCount * 4) + 0] = r.x;
            boundBoxArray[(polygonCount * 4) + 1] = r.y;
            boundBoxArray[(polygonCount * 4) + 2] = r.width;
            boundBoxArray[(polygonCount * 4) + 3] = r.height;

            polygonCount++;
        }
    }

    public void enqueueTasks(cl_event externalTask)
    {
        if (size == 0) return;

        //write all data to device
        setupN();
        cl_event[] writingEvents = new cl_event[4];

        int i = 0;
        writingEvents[i++] = setupBoundboxArray(taskEvent);
        writingEvents[i++] = setupZBuffer(taskEvent);
        writingEvents[i++] = externalTask;
        writingEvents[i++] = setupIndexArray(taskEvent);

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

    //OUTPUT ARGUMENTS
    private void setupOutputMemory()
    {
        IJoclMemory m = dynamic.put(null, outMap, outMapBuffer, 0, CL_MEM_WRITE_ONLY);
        clSetKernelArg(kernel, outArg, Sizeof.cl_mem, m.getObject());
    }
    //OUTPUT ARGUMENT END

    private void setupN()
    {
        clSetKernelArg(kernel, nArg, Sizeof.cl_int, Pointer.to(new int[]{size}));
    }

    //----------EXTERNAL
    public void setupPolygonArray(IJoclMemory m)
    {
        clSetKernelArg(kernel, polygonArrayArg, Sizeof.cl_mem, m.getObject());
    }

    public void setupPolygonStartArray(IJoclMemory m)
    {
        clSetKernelArg(kernel, polygonStartArrayArg, Sizeof.cl_mem, m.getObject());
    }
    //----------EXTERNAL

    private cl_event setupBoundboxArray(cl_event task)
    {
        IJoclMemory m = dynamic.put(task, null, boundBoxArray, 0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, boundBoxArrayArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }

    private cl_event setupZBuffer(cl_event task)
    {
        IJoclMemory m = dynamic.put(task, zMapOut, zMapBuffer, 0, CL_MEM_READ_WRITE);
        clSetKernelArg(kernel, zMapArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }

    private cl_event setupIndexArray(cl_event task)
    {
        IJoclMemory m = dynamic.put(task, indexArray, indexArrayData, 0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, indexArrayArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }
}
