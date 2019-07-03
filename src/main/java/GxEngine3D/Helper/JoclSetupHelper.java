package GxEngine3D.Helper;

import org.jocl.*;

import static org.jocl.CL.*;
import static org.jocl.CL.clCreateContext;

public class JoclSetupHelper {

    public static cl_platform_id createPlatform()
    {
        final int platformIndex = 0;

        // Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        return platform;
    }

    public static cl_device_id createDevice(cl_platform_id platform)
    {
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        return device;
    }

    public static cl_context createContext(cl_device_id device, cl_platform_id platform)
    {
        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Create a context for the selected device
        cl_context context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        return context;
    }

    public static cl_command_queue createCommandQueue(cl_context context, cl_device_id device, boolean profiling)
    {
        cl_queue_properties p = new cl_queue_properties();
        if (profiling) {
            p.addProperty(CL_QUEUE_PROPERTIES, CL_QUEUE_PROFILING_ENABLE);
        }

        cl_command_queue commandQueue = clCreateCommandQueueWithProperties(context, device, p, null);

        return commandQueue;
    }
}
