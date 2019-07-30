package TextureGraphics.UpdateScene;

import TextureGraphics.ExecutionStatistics;
import TextureGraphics.JoclProgram;
import TextureGraphics.JoclSetup;
import TextureGraphics.Memory.AsyncJoclMemory;
import TextureGraphics.Memory.BufferHelper;
import TextureGraphics.Memory.IJoclMemory;
import TextureGraphics.Memory.MemoryHandler;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_event;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import static org.jocl.CL.*;

public class UpdateCulling extends JoclProgram {

    int screenWidth, screenHeight;

    ArrayList<cl_event> taskEvents;

    //names to retrieve arguments by
    String polygonStart = "PolygonStart", indexArray = "IndexArray",
            screenSize = "ScreenSize", polygonArray = "PolygonArray";

    //arg indices
    int screenSizeArg = 0,
            polygonArrayArg = 1,
            polyStartArrayArg = 2,
            clipArrayArg = 3,
            indexArrayArg = 4
    ;

    int polyCount;

    int[] polygonStartArrayData, polygonArrayData;

    ByteBuffer indexArrayData, indexArrayDataInit;

    cl_event task;

    String kernelPath = "resources/Kernels/UpdateScene/UpdateCulling.cl", kernelMethod = "updateScene";

    public UpdateCulling(int screenWidth, int screenHeight)
    {
        this.profiling = true;

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create(kernelPath, kernelMethod);

        taskEvents = new ArrayList<>();

        super.start();
    }

    public UpdateCulling(int screenWidth, int screenHeight, JoclSetup setup)
    {
        this.profiling = setup.isProfiling();

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create(kernelPath, kernelMethod, setup);

        taskEvents = new ArrayList<>();

        super.start();
    }

    public IJoclMemory getPolygonArray()
    {
        return cached.get(polygonArray);
    }

    public IJoclMemory getPolygonStartArray()
    {
        return cached.get(polygonStart);
    }

    public int[] getPolygonStartData()
    {
        return polygonStartArrayData;
    }

    public IntBuffer getIndexArrayData()
    {
        indexArrayData.order(ByteOrder.nativeOrder());
        return indexArrayData.asIntBuffer();
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

    public void prepare(ArrayList<double[]> points, ArrayList<int[]> polygons)
    {
        int pointCount = 0;
        for (int[] p: polygons)
        {
            pointCount += p.length;
        }
        polyCount = polygons.size();

        polygonArrayData = new int[points.size() * 3];//indices

        indexArrayData = ByteBuffer.allocateDirect(polyCount * Sizeof.cl_int);

        int[] indexStart = new int[polyCount];
        Arrays.fill(indexStart, 0);
        indexArrayDataInit = BufferHelper.createBuffer(indexStart);

        polygonStartArrayData = new int[polyCount+1];
        polygonStartArrayData[polyCount] = pointCount;

        addPolygons(polygons);
    }

    private void addPolygons(ArrayList<int[]> polygons)
    {
        int localPolyCount = 0, memoryOffset = 0;
        for (int[] polygon: polygons)
        {
            polygonStartArrayData[localPolyCount] = memoryOffset;
            localPolyCount++;

            int size = polygon.length;
            System.arraycopy(polygon, 0, polygonArrayData, memoryOffset, size);
            memoryOffset += size;
        }
    }

    public void enqueue()
    {
        setupOutputMemory();

        taskEvents.add(task);

        cl_event[] waitingEvents = new cl_event[2];

        waitingEvents[0] = setupPolygonStartArray(task);
        waitingEvents[1] = setupPolygonArray(task);

        long[] globalWorkSize = { polyCount };

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
        readTasks = new cl_event[1];
        for (int i=0;i<readTasks.length;i++)
        {
            readTasks[i] = new cl_event();
        }

        cl_event[] executionTasks = new cl_event[taskEvents.size()];
        taskEvents.toArray(executionTasks);

        int i = 0;
        clEnqueueReadBuffer(commandQueue, dynamic.get(indexArray).getRawObject(), false, 0,
                Sizeof.cl_int * polyCount, Pointer.to(indexArrayData), executionTasks.length, executionTasks, readTasks[i++]);

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
        recreateOutputMemory();
        setupOutArgs();
    }

    private void recreateOutputMemory()
    {
        dynamic.put(null, indexArray, indexArrayDataInit, 0, CL_MEM_WRITE_ONLY);
    }

    private void setupOutArgs()
    {
        clSetKernelArg(kernel, indexArrayArg, Sizeof.cl_mem, dynamic.get(indexArray).getObject());
    }
    //OUTPUT ARGUMENT END

    //EXTERNAL
    public void setupClipArray(IJoclMemory m)
    {
        clSetKernelArg(kernel, clipArrayArg, Sizeof.cl_mem, m.getObject());
    }
    //EXTERNAL

    private cl_event setupPolygonStartArray(cl_event task)
    {
        IJoclMemory m = cached.put(task, polygonStart, polygonStartArrayData,0, CL_MEM_READ_ONLY);
        setPolygonStartArg(m);
        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }

    private cl_event setupPolygonArray(cl_event task)
    {
        IJoclMemory m = cached.put(task, polygonArray, polygonArrayData,0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, polygonArrayArg, Sizeof.cl_mem, m.getObject());
        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }

    private void setPolygonStartArg(IJoclMemory m)
    {
        clSetKernelArg(kernel, polyStartArrayArg, Sizeof.cl_mem, m.getObject());
    }
}
