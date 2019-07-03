package TextureGraphics;

import GxEngine3D.Helper.JoclSetupHelper;
import TextureGraphics.Memory.CachedMemoryHandler;
import TextureGraphics.Memory.MemoryHandler;
import org.jocl.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.jocl.CL.*;

public abstract class JoclProgram {

    protected cl_kernel kernel;

    protected cl_context context;
    protected cl_command_queue commandQueue;

    protected boolean profiling = false;
    cl_device_id device;

    //dynamic memory gets refreshed from round to round
    //cached memory stays for the lifetime of the program object, but can be added at anytime
    //static memory does not change after init
    protected MemoryHandler dynamic, immutable, cached;

    //after newest nvidia drivers, the cleanup became a race condition. syncing threads should remove this
    Thread cleanupThread = new Thread();

    protected void start()
    {
        initStaticMemory();
        initDynamicMemory();
        initCachedMemory();
    }

    protected void initStaticMemory()
    {
        immutable = new MemoryHandler(context, commandQueue);
    }

    public void setup()
    {
        initDynamicMemory();
    }

    public void initCachedMemory()
    {
        cached = new CachedMemoryHandler(context, commandQueue);
    }

    protected void initDynamicMemory()
    {
        if (cleanupThread.isAlive()) {
            try {
                cleanupThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        dynamic = new MemoryHandler(context, commandQueue);
    }

    protected void finish()
    {
        cleanupThread = new Thread(new Runnable() {
            @Override
            public void run() {
                dynamic.releaseAll();
            }
        });
        cleanupThread.start();
    }

    protected void create(String sourceFile, String kernalProgram, JoclSetup sharedProgram)
    {
        this.profiling = sharedProgram.isProfiling();
        create(sourceFile, kernalProgram, sharedProgram.getContext(), sharedProgram.getCommandQueue());
    }

    protected void create(String sourceFile, String kernalProgram, JoclProgram sharedProgram)
    {
        this.profiling = sharedProgram.profiling;
        create(sourceFile, kernalProgram, sharedProgram.context, sharedProgram.commandQueue);
    }

    protected void create(String sourceFile, String kernalProgram)
    {
        cl_platform_id platform = JoclSetupHelper.createPlatform();
        cl_device_id device = JoclSetupHelper.createDevice(platform);

        cl_context context = JoclSetupHelper.createContext(device, platform);

        cl_command_queue commandQueue = JoclSetupHelper.createCommandQueue(context, device, this.profiling);

        create(sourceFile, kernalProgram, context, commandQueue);
    }

    private void create(String sourceFile, String kernalProgram, cl_context context, cl_command_queue commandQueue)
    {
        this.context = context;
        this.commandQueue = commandQueue;
        // Program Setup
        String source = readFile(sourceFile);

        // Create the program
        cl_program cpProgram = clCreateProgramWithSource(context, 1,
                new String[]{ source }, null, null);

        // Build the program
        clBuildProgram(cpProgram, 0, null, "-cl-mad-enable", null, null);

        // Create the kernel
        kernel = clCreateKernel(cpProgram, kernalProgram, null);
    }

    private String readFile(String fileName)
    {
        try
        {
            fileName = getClass().getClassLoader().getResource(fileName).getFile();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(fileName)));
            StringBuffer sb = new StringBuffer();
            String line = null;
            while (true)
            {
                line = br.readLine();
                if (line == null)
                {
                    break;
                }
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }
}
