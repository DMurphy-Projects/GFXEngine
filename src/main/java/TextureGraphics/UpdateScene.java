package TextureGraphics;

import GxEngine3D.Helper.ArrayHelper;
import GxEngine3D.Helper.Maths.VectorCalc;
import GxEngine3D.Helper.PerformanceTimer;
import TextureGraphics.Memory.AsyncJoclMemory;
import TextureGraphics.Memory.BufferHelper;
import TextureGraphics.Memory.IJoclMemory;
import TextureGraphics.Memory.MemoryHandler;
import TextureGraphics.Memory.Texture.ITexture;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_event;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

import static org.jocl.CL.*;

//could be optimised further
//does not have the sparse pixel problem of predecessors however this is not robust against the flaws in the projection matrix
public class UpdateScene extends JoclProgram{

    int screenWidth, screenHeight;

    ArrayList<cl_event> taskEvents;

    //names to retrieve arguments by
    String relativeArray = "Relative", clipArray = "Clip", screenArray = "Screen",
            screenSize = "ScreenSize";

    //arg indices
    int screenSizeArg = 0,
            relativeArrayArg = 1,
            polyStartArrayArg = 2,
            matrixArg = 3,
            clipArrayArg = 4,
            screenArrayArg = 5
    ;

    int pointCount, memoryOffset, polyCount;

    double[] relativeArrayData, matrix;
    int[] polygonStartArrayData;

    ByteBuffer clipArrayData, screenArrayData;

    cl_event task;

    public UpdateScene(int screenWidth, int screenHeight)
    {
        this.profiling = false;

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create("resources/Kernels/SceneUpdate.cl", "updateScene");

        taskEvents = new ArrayList<>();

        super.start();
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

        pointCount = 0;
        polyCount = 0;
        memoryOffset = 0;
    }

    public void prepare(ArrayList<double[][]> polygons)
    {
        int count = 0;
        for (double[][] polygon: polygons)
        {
            count += polygon.length;
        }

        relativeArrayData = new double[count * 3];
        clipArrayData = ByteBuffer.allocateDirect(count * 3 * Sizeof.cl_double);
        screenArrayData = ByteBuffer.allocateDirect(count * 3 * Sizeof.cl_double);

        int polyCount = polygons.size();
        polygonStartArrayData = new int[polyCount+1];
        polygonStartArrayData[polyCount] = (count * 3) - 1;
    }

    public void setMatrix(double[] flatMatrix)
    {
        matrix = flatMatrix;
    }

    public void addPolygon(double[][] polygon)
    {
        polygonStartArrayData[polyCount] = memoryOffset;

        for (double[] p: polygon)
        {
            int size = p.length;
            System.arraycopy(p, 0, relativeArrayData, memoryOffset, size);
            memoryOffset += size;

            pointCount++;
        }

        polyCount++;
    }

    public void enqueue()
    {
        setupOutputMemory();

        taskEvents.add(task);

        cl_event[] waitingEvents = new cl_event[3];

        waitingEvents[0] = setupRelativeArray(task);
        waitingEvents[1] = setupPolygonStartArray(task);
        waitingEvents[2] = setupMatrix(task);

        long[] globalWorkSize = { polyCount };

        //TODO do enqueue
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                globalWorkSize, null, waitingEvents.length, waitingEvents, task);
    }

    public void update()
    {
        if (taskEvents.size() > 0) {
            cl_event[] events = new cl_event[taskEvents.size()];
            taskEvents.toArray(events);

            clWaitForEvents(events.length, events);
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
    private void readData()
    {
        cl_event readTask1 = new cl_event();
        cl_event readTask2 = new cl_event();

        clEnqueueReadBuffer(commandQueue, dynamic.get(clipArray).getRawObject(), false, 0,
                Sizeof.cl_double * pointCount * 3, Pointer.to(clipArrayData), 0, null, readTask1);

        clEnqueueReadBuffer(commandQueue, dynamic.get(screenArray).getRawObject(), false, 0,
                Sizeof.cl_double * pointCount * 3, Pointer.to(screenArrayData), 0, null, readTask2);

        clWaitForEvents(2, new cl_event[]{readTask1, readTask2});
    }

    private void setupScreenSizeArgs()
    {
        clSetKernelArg(kernel, screenSizeArg, Sizeof.cl_mem, immutable.get(screenSize).getObject());
    }

    //OUTPUT ARGUMENTS
    private void setupOutputMemory()
    {
        //each point has 3 components, (x, y, z)
        recreateOutputMemory(pointCount * 3);
        setupOutArgs();
    }

    private void recreateOutputMemory(int size)
    {
        dynamic.put(null, clipArray, size * Sizeof.cl_double, CL_MEM_WRITE_ONLY);
        dynamic.put(null, screenArray, size * Sizeof.cl_double, CL_MEM_WRITE_ONLY);
    }

    private void setupOutArgs()
    {
        clSetKernelArg(kernel, clipArrayArg, Sizeof.cl_mem, dynamic.get(clipArray).getObject());
        clSetKernelArg(kernel, screenArrayArg, Sizeof.cl_mem, dynamic.get(screenArray).getObject());
    }
    //OUTPUT ARGUMENT END

    private cl_event setupRelativeArray(cl_event task)
    {
        IJoclMemory m = dynamic.put(task, null, relativeArrayData,0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, relativeArrayArg, Sizeof.cl_mem, m.getObject());
        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }

    private cl_event setupMatrix(cl_event task)
    {
        IJoclMemory m = dynamic.put(task, null, matrix,0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, matrixArg, Sizeof.cl_mem, m.getObject());
        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }

    private cl_event setupPolygonStartArray(cl_event task)
    {
        IJoclMemory m = dynamic.put(task, null, polygonStartArrayData,0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, polyStartArrayArg, Sizeof.cl_mem, m.getObject());
        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }
}
