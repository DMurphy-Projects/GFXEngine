package TextureGraphics;

import TextureGraphics.Memory.AsyncJoclMemory;
import TextureGraphics.Memory.BufferHelper;
import TextureGraphics.Memory.IJoclMemory;
import TextureGraphics.Memory.MemoryHandler;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_event;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import static org.jocl.CL.*;

public class UpdateRefPoint extends JoclProgram{

    int screenWidth, screenHeight;

    ArrayList<cl_event> taskEvents;

    //names to retrieve arguments by
    String relativeArray = "Relative",
            clipArray = "Clip",
            screenArray = "Screen",
            screenSize = "ScreenSize";

    //arg indices
    int screenSizeArg = 0,
            relativeArrayArg = 1,
            matrixArg = 2,
            clipArrayArg = 3,
            screenArrayArg = 4
    ;

    int pointCount;

    double[] relativeArrayData, matrix;

    ByteBuffer clipArrayData, screenArrayData;

    cl_event task;

    boolean relativeUpdated = false;

    String kernelPath = "resources/Kernels/SceneUpdate/UpdateRefPoint.cl", kernelMethod = "updateScene";

    public UpdateRefPoint(int screenWidth, int screenHeight)
    {
        this.profiling = true;

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create(kernelPath, kernelMethod);

        taskEvents = new ArrayList<>();

        super.start();
    }

    public UpdateRefPoint(int screenWidth, int screenHeight, JoclSetup setup)
    {
        this.profiling = setup.isProfiling();

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create(kernelPath, kernelMethod, setup);

        taskEvents = new ArrayList<>();

        super.start();
    }

    public DoubleBuffer getScreenPolygonBuffer()
    {
        screenArrayData.order(ByteOrder.nativeOrder());
        return screenArrayData.asDoubleBuffer();
    }

    public DoubleBuffer getClipPolygonBuffer()
    {
        clipArrayData.order(ByteOrder.nativeOrder());
        return clipArrayData.asDoubleBuffer();
    }

    public IJoclMemory getScreenPolygonArray()
    {
        return dynamic.get(screenArray);
    }
    public IJoclMemory getRelativePolygonArray()
    {
        return cached.get(relativeArray);
    }
    public IJoclMemory getClipPolygonArray()
    {
        return dynamic.get(clipArray);
    }

    public cl_event getTask()
    {
        return task;
    }

    @Override
    protected void initStaticMemory() {
        super.initStaticMemory();

        immutable.put(null, screenSize, new int[]{screenWidth, screenHeight}, 0, CL_MEM_READ_ONLY);
        setupScreenSizeArgs();
    }

    @Override
    public void initCachedMemory() {
        //don't need the value based cache
        this.cached = new MemoryHandler(this.context, this.commandQueue);
    }

    @Override
    public void setup() {
        super.setup();
        taskEvents.clear();

        task = new cl_event();
    }

    public void prepare(ArrayList<double[]> points)
    {
        pointCount = points.size();

        relativeArrayData = new double[pointCount * 3];//points

        clipArrayData = ByteBuffer.allocateDirect(pointCount * 3 * Sizeof.cl_double);
        screenArrayData = ByteBuffer.allocateDirect(pointCount * 3 * Sizeof.cl_double);

        createRelativePointsData(points);

        relativeUpdated = true;
    }

    public void setMatrix(double[] flatMatrix)
    {
        matrix = flatMatrix;
    }

    private void createRelativePointsData(ArrayList<double[]> points)
    {
        int memoryOffset = 0;
        for (double[] p: points)
        {
            int size = p.length;
            System.arraycopy(p, 0, relativeArrayData, memoryOffset, size);
            memoryOffset += size;
        }
        int i = 0;
    }

    public void enqueue()
    {
        setupOutputMemory();

        taskEvents.add(task);

        cl_event[] waitingEvents = new cl_event[relativeUpdated?2:1];

        int i = 0;
        if (relativeUpdated)
        {
            waitingEvents[0] = setupRelativeArray(task);

            relativeUpdated = false;
            i += 1;
        }
        waitingEvents[i] = setupMatrix(task);

        long[] globalWorkSize = { pointCount };

        //TODO do enqueue
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                globalWorkSize, null, waitingEvents.length, waitingEvents, task);
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

    public void update()
    {
        if (waitOnFinishing()) {

            readData();

            if (profiling) {
                ExecutionStatistics stats = new ExecutionStatistics();
                for (cl_event event : taskEvents) {
                    stats.addEntry("", event, ExecutionStatistics.Formats.Nano);
                }
                stats.printTotal();
            }
        }

        finish();
    }

    //JOCL handling functions
    cl_event[] readTasks;
    public void readData()
    {
        readTasks = new cl_event[2];
        for (int i=0;i<readTasks.length;i++)
        {
            readTasks[i] = new cl_event();
        }

        cl_event[] executionTasks = new cl_event[taskEvents.size()];
        taskEvents.toArray(executionTasks);

        int i = 0;
        clEnqueueReadBuffer(commandQueue, dynamic.get(screenArray).getRawObject(), false, 0,
                Sizeof.cl_double * pointCount * 3, Pointer.to(screenArrayData), executionTasks.length, executionTasks, readTasks[i++]);

        clEnqueueReadBuffer(commandQueue, dynamic.get(clipArray).getRawObject(), false, 0,
                Sizeof.cl_double * pointCount * 3, Pointer.to(clipArrayData), executionTasks.length, executionTasks, readTasks[i++]);
    }

    public void waitOnReadTasks()
    {
        clWaitForEvents(readTasks.length, readTasks);
    }

    private void setupScreenSizeArgs()
    {
        clSetKernelArg(kernel, screenSizeArg, Sizeof.cl_mem, immutable.get(screenSize).getObject());
    }

    //OUTPUT ARGUMENTS
    private void setupOutputMemory()
    {
        //each point has 3 components, (x, y, z)
        recreateOutputMemory(pointCount);
        setupOutArgs();
    }

    private void recreateOutputMemory(int size)
    {
        dynamic.put(null, clipArray, size * 3 * Sizeof.cl_double, CL_MEM_WRITE_ONLY);
        dynamic.put(null, screenArray, size * 3 * Sizeof.cl_double, CL_MEM_WRITE_ONLY);
    }

    private void setupOutArgs()
    {
        clSetKernelArg(kernel, clipArrayArg, Sizeof.cl_mem, dynamic.get(clipArray).getObject());
        clSetKernelArg(kernel, screenArrayArg, Sizeof.cl_mem, dynamic.get(screenArray).getObject());
    }
    //OUTPUT ARGUMENT END

    private cl_event setupRelativeArray(cl_event task)
    {
        IJoclMemory m = cached.put(task, relativeArray, relativeArrayData,0, CL_MEM_READ_ONLY);
        setRelativeArrayArg(m);
        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }

    private cl_event setupMatrix(cl_event task)
    {
        IJoclMemory m = dynamic.put(task, null, matrix,0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, matrixArg, Sizeof.cl_mem, m.getObject());
        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }

    private void setRelativeArrayArg(IJoclMemory m)
    {
        clSetKernelArg(kernel, relativeArrayArg, Sizeof.cl_mem, m.getObject());
    }
}
