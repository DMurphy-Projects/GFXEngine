package TextureGraphics;

import TextureGraphics.Memory.IJoclMemory;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_event;

import java.util.ArrayList;

import static org.jocl.CL.*;

public class BackgroundFragment extends  JoclProgram{

    int screenWidth, screenHeight;

    //names to retrieve arguments by
    String screenSize = "ScreenSize";

    //arg indices
    int screenSizeArg = 0,
            outMapArg = 1,
            outPixelArg = 2
                    ;

    ArrayList<cl_event> taskEvents;
    cl_event taskEvent;

    public BackgroundFragment(int screenWidth, int screenHeight, JoclSetup setup)
    {
        this.profiling = setup.isProfiling();

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create("resources/Kernels/FragmentRender/BackgroundFragment.cl", "render",
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

    public cl_event getTask()
    {
        return taskEvent;
    }

    public void enqueueTasks(cl_event externalTask)
    {
        int size = 1;
        //write all data to device
        cl_event[] writingEvents = new cl_event[size];

        int i = 0;
        writingEvents[i++] = externalTask;

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
    public void setupPixelOut(Pointer pointer)
    {
        clSetKernelArg(kernel, outPixelArg, Sizeof.cl_mem, pointer);
    }
    //----------EXTERNAL
}
