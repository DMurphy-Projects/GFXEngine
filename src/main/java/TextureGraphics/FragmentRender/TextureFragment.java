package TextureGraphics.FragmentRender;

import TextureGraphics.JoclProgram;
import TextureGraphics.JoclSetup;
import TextureGraphics.Memory.AsyncJoclMemory;
import TextureGraphics.Memory.IJoclMemory;
import TextureGraphics.Memory.MemoryHandler;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_event;

import java.util.ArrayList;

import static org.jocl.CL.*;

//this implementation is based on the GpuRendererIterateColorPolygon
//the handling of the polygon points will be external
//
public class TextureFragment extends JoclProgram {

    int screenWidth, screenHeight;

    //names to retrieve arguments by
    String outMap = "OutMap", screenSize = "ScreenSize";

    //arg indices
    int screenSizeArg = 0,
            outMapArg = 1,
            metaInfoArg = 2,
            outPixelArg = 3,
            indexArrayArg = 4,
            textureDataArg = 5,
            textureInfoArg = 6,
            textureMetaArg = 7,
            zMapArg = 8,
            relativeArrayArg = 9,
            textureRelativeArrayArg = 10,
            polygonStartArrayArg = 11,
            inverseMatrixArg = 12
                    ;

    ArrayList<cl_event> taskEvents;
    cl_event taskEvent;

    public TextureFragment(int screenWidth, int screenHeight, JoclSetup setup)
    {
        this.profiling = setup.isProfiling();

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create("resources/Kernels/FragmentRender/TextureFragment.cl", "render",
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
    public void initCachedMemory() {
        cached = new MemoryHandler(context, commandQueue);
    }

    @Override
    public void setup() {
        super.setup();
        taskEvents = new ArrayList<>();

        taskEvent = new cl_event();
    }

    public double[] flatTRelative;
    public void prepareTextureRelative(ArrayList<double[][]> tRelative)
    {
        int size = 0;
        for (double[][] poly: tRelative)
        {
            size += poly.length * 3;
        }
        flatTRelative = new double[size];

        int offset = 0;
        for (double[][] poly: tRelative)
        {
            for (double[] point: poly)
            {
                int len = point.length;
                System.arraycopy(point, 0, flatTRelative, offset, len);
                offset += len;
            }
        }

        IJoclMemory m = cached.put(null, null, flatTRelative, 0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, textureRelativeArrayArg, Sizeof.cl_mem, m.getObject());
    }

    double[] inverseMatrix;
    public void setInverseMatrix(double[] i)
    {
        inverseMatrix = i;
    }

    public void enqueueTasks(cl_event externalTask)
    {
        int size = 2;
        //write all data to device
        cl_event[] writingEvents = new cl_event[size];

        int i = 0;
        writingEvents[i++] = externalTask;
        writingEvents[i++] = setupInverseMatrix();

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
    public void setupTextureDataArray(Pointer pointer)
    {
        clSetKernelArg(kernel, textureDataArg, Sizeof.cl_mem, pointer);
    }
    public void setupTextureInfoArray(Pointer pointer)
    {
        clSetKernelArg(kernel, textureInfoArg, Sizeof.cl_mem, pointer);
    }
    public void setupTextureMetaArray(Pointer pointer)
    {
        clSetKernelArg(kernel, textureMetaArg, Sizeof.cl_mem, pointer);
    }
    public void setupRelativeArray(IJoclMemory m)
    {
        clSetKernelArg(kernel, relativeArrayArg, Sizeof.cl_mem, m.getObject());
    }
    public void setupZMap(IJoclMemory m)
    {
        clSetKernelArg(kernel, zMapArg, Sizeof.cl_mem, m.getObject());
    }
    public void setupTextureRelativeArray(Pointer pointer)
    {
        clSetKernelArg(kernel, textureRelativeArrayArg, Sizeof.cl_mem, pointer);
    }
    public void setupPolygonStartArray(IJoclMemory m)
    {
        clSetKernelArg(kernel, polygonStartArrayArg, Sizeof.cl_mem, m.getObject());
    }
    //----------EXTERNAL

    private cl_event setupInverseMatrix()
    {
        IJoclMemory m = dynamic.put(taskEvent, null, inverseMatrix, 0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, inverseMatrixArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }
}
