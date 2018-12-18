package TextureGraphics;

import org.jocl.*;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.jocl.CL.*;

public abstract class JoclRenderer {


    protected cl_kernel kernel;

    protected cl_context context;
    protected cl_command_queue commandQueue;

    protected cl_mem[] inArrays, outArrays;

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

        long properties = 0;//CL_QUEUE_PROFILING_ENABLE;

        // Create a command-queue for the selected device
        commandQueue =
                clCreateCommandQueue(context, device, properties, null);

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

    public JoclTexture createTexture(String path)
    {
        return new JoclTexture(path, context, commandQueue);
    }

    //should either schedule or draw the polygon given
    public abstract void render(double[][] polygon, double[][] textureAnchor, JoclTexture texture);
    //should use the information generated from rendering the polygons to create a single screen
    //NOTE: this step may be unnecessary depending upon implementation
    public abstract BufferedImage createImage();
    //should prepare the renderer for rendering a polygon
    public abstract void setup();
}
