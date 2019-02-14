package TextureGraphics;

import GxEngine3D.Helper.ValueBasedIdGen;
import TextureGraphics.Memory.JoclMemory;
import org.jocl.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import static org.jocl.CL.*;

public abstract class JoclProgram {

    protected cl_kernel kernel;

    protected cl_context context;
    protected cl_command_queue commandQueue;

    protected boolean profiling = false;
    cl_device_id device;

    HashMap<String, Integer> dynamicNames = new HashMap<>();
    HashMap<String, Integer> staticNames = new HashMap<>();

    HashMap<String, JoclMemory> cachedMemory = new HashMap<>();

    //dynamic memory gets refreshed from round to round
    //cached memory stays for the lifetime of the program object, but can be added at anytime
    //static memory does not change after init
    ArrayList<JoclMemory> dynamicMemory = new ArrayList<>();
    protected cl_mem[] staticMemory;

    //after newest nvidia drivers, the cleanup became a race condition. syncing threads should remove this
    Thread cleanupThread = new Thread();

    protected abstract void initStaticMemory();

    protected void start()
    {
        initStaticMemory();
        initDynamicMemory();
    }

    public void setup()
    {
        initDynamicMemory();
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
        dynamicMemory = new ArrayList<>();
        dynamicNames = new HashMap<>();
    }

    protected void finish()
    {
        cleanupThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (JoclMemory m:dynamicMemory)
                {
                    m.release();
                }
            }
        });
        cleanupThread.start();
    }

    protected JoclMemory getDynamic(String name)
    {
        return dynamicMemory.get(dynamicNames.get(name));
    }

    protected cl_mem getStatic(String name)
    {
        return staticMemory[staticNames.get(name)];
    }

    protected JoclMemory setCachedMemoryArg(cl_event task, double[] arr, long type)
    {
        String id = ValueBasedIdGen.generate(arr);
        if (cachedMemory.containsKey(id))
        {
            return cachedMemory.get(id);
        }
        else {
            JoclMemory m = JoclMemory.createAsync(context, commandQueue, task, arr, type);
            cachedMemory.put(id, m);
            return m;
        }
    }

    protected void setStaticMemoryArg(int index, int size, long type, String name)
    {
        checkStaticNameExists(name);
        staticNames.put(name, staticNames.size());
        setStaticMemoryArg(index, size, type);
    }

    protected void setStaticMemoryArg(int index, int size, long type)
    {
        cl_mem m = clCreateBuffer(context, type,
                size, null, null);
        staticMemory[index] = m;
    }

    protected void setStaticMemoryArg(int index, int[] array, String name)
    {
        checkStaticNameExists(name);
        staticNames.put(name, staticNames.size());
        setStaticMemoryArg(index, array);
    }

    protected void setStaticMemoryArg(int index, int[] array)
    {
        cl_mem m = clCreateBuffer(context, CL_MEM_READ_ONLY,
                array.length * Sizeof.cl_int, null, null);
        clEnqueueWriteBuffer(commandQueue, m, true, 0,
                array.length * Sizeof.cl_int, Pointer.to(array), 0, null, null);
        staticMemory[index] = m;
    }

    private void checkStaticNameExists(String name)
    {
        if (staticNames.containsKey(name))
        {
            System.out.println(String.format("Static Name '%s' already exists, releasing previous object ", name));
            clReleaseMemObject(staticMemory[staticNames.get(name)]);
        }
    }

    private void checkDynamicNameExists(String name)
    {
        if (dynamicNames.containsKey(name))
        {
            System.out.println(String.format("Dynamic Name '%s' already exists, releasing previous object ", name));
            dynamicMemory.get(dynamicNames.get(name)).release();
        }
    }

    protected JoclMemory setMemoryArg(double[] arr, long type, String name)
    {
        checkDynamicNameExists(name);
        dynamicNames.put(name, dynamicMemory.size());
        return setMemoryArg(arr, type);
    }

    protected JoclMemory setMemoryArg(int size, long type, String name)
    {
        checkDynamicNameExists(name);
        dynamicNames.put(name, dynamicMemory.size());
        return setMemoryArg(size, type);
    }

    protected JoclMemory setMemoryArg(int size, long type)
    {
        JoclMemory m = JoclMemory.createEmpty(context, commandQueue, size, type);
        dynamicMemory.add(m);
        return m;
    }

    protected JoclMemory setMemoryArg(double[] arr, long type)
    {
        JoclMemory m = JoclMemory.createBlocking(context, commandQueue, arr, type);
        dynamicMemory.add(m);
        return m;
    }

    protected JoclMemory setMemoryArg(cl_event task, double[] arr, long type, String name)
    {
        checkDynamicNameExists(name);
        dynamicNames.put(name, dynamicMemory.size());
        return setMemoryArg(task, arr, type);
    }

    protected JoclMemory setMemoryArg(cl_event task, double[] arr, long type)
    {
        JoclMemory m = JoclMemory.createAsync(context, commandQueue, task, arr, type);
        dynamicMemory.add(m);
        return m;
    }

    protected JoclMemory setMemoryArg(cl_event task, int[] arr, long type)
    {
        JoclMemory m = JoclMemory.createAsync(context, commandQueue, task, arr, type);
        dynamicMemory.add(m);
        return m;
    }

    protected void create(String sourceFile, String kernalProgram)
    {
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        // Create a context for the selected device
        context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        cl_queue_properties p = new cl_queue_properties();
        if (profiling) {
            p.addProperty(CL_QUEUE_PROPERTIES, CL_QUEUE_PROFILING_ENABLE);
            this.device = device;
        }

        commandQueue = clCreateCommandQueueWithProperties(context, device, p, null);

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
