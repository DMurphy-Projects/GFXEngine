package Programs.KernalTestPrograms;

import GxEngine3D.Camera.Camera;
import GxEngine3D.Helper.Maths.MatrixHelper;
import GxEngine3D.Model.Matrix.Matrix;
import GxEngine3D.View.ViewHandler;
import org.jocl.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;

import static org.jocl.CL.*;

public class JoclTest04
{
    public static void main(String args[])
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                new JoclTest04(500,500);
            }
        });
    }

    private BufferedImage image;
    private int screenWidth = 0;
    private int screenHeight = 0;

    private JComponent imageComponent;

    private cl_context context;

    private cl_command_queue commandQueue;

    private cl_kernel kernel;

    private cl_mem pixelMem, screenSizeMem, textureMem, textureSizeMem, debugBufferMem;
    private cl_mem[] triangleArgs = new cl_mem[7];

    int tWidth, tHeight;

    double[][] relativePoints, textureRelativePoints, clipPoints;
    double[] debugBuffer = new double[10];

    Camera camera;
    ViewHandler vH;

    double roll = 0, rollStep = 0;
    double yaw = 0, yawStep = 0;
    double pitch = 0, pitchStep = 0.1;

    public JoclTest04(int width, int height)
    {
        screenWidth = width;
        screenHeight = height;

        // Create the image and the component that will paint the image
        image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
        imageComponent = new JPanel()
        {
            private static final long serialVersionUID = 1L;
            public void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                g.drawImage(image, 0,0,this);
            }
        };

        // Initialize the mouse interaction
        initInteraction();

        // Initialize OpenCL
        initCL();

        //Initialise texture
        String path = "resources/Textures/default.png";
        try {
            BufferedImage texture = ImageIO.read(new File(getClass().getClassLoader().getResource(path).getFile()));
            this.tWidth = texture.getWidth();
            this.tHeight = texture.getHeight();

            textureSizeMem = clCreateBuffer(context, CL_MEM_READ_ONLY,
                    2 * Sizeof.cl_int, null, null);
            clEnqueueWriteBuffer(commandQueue, textureSizeMem, true, 0,
                    2 * Sizeof.cl_int, Pointer.to(new int[]{tWidth, tHeight}), 0, null, null);

            int[] textureArr = texture.getRGB(0, 0, texture.getWidth(), texture.getHeight(), null, 0, texture.getWidth());
            textureMem = clCreateBuffer(context, CL_MEM_READ_ONLY,
                    tWidth * tHeight * Sizeof.cl_uint, null, null);
            clEnqueueWriteBuffer(commandQueue, textureMem, true, 0,
                    tWidth * tHeight * Sizeof.cl_uint, Pointer.to(textureArr), 0, null, null);


        } catch (IOException e) {
        }

        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(screenSizeMem));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(textureMem));
        clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(textureSizeMem));

        // Create the main frame
        JFrame frame = new JFrame("JOCL Simple Mandelbrot");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        imageComponent.setPreferredSize(new Dimension(width, height));
        frame.add(imageComponent, BorderLayout.CENTER);
        frame.pack();

        //initialise the shapes that we're going to use
        initShape();
        updateShape();

        // Initial image update
        updateImage();

        frame.setVisible(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    double dir = 1;
                    double[][] tran = MatrixHelper.setupTranslateMatrix(0, 0, 0);
                    double[][] rotate = MatrixHelper.setupFullRotation(pitch += pitchStep * dir, yaw += yawStep * dir, roll += rollStep * dir);
                    double[][] scale = new double[4][4];
                    scale[0][0] = 1;
                    scale[1][1] = 1;
                    scale[2][2] = 1;
                    scale[3][3] = 1;
                    updateShape(tran, rotate, scale);
                    updateImage();
                }
            }
        });
    }

    private void initCL()
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

        long properties = CL_QUEUE_PROFILING_ENABLE;

        // Create a command-queue for the selected device
        commandQueue =
                clCreateCommandQueue(context, device, properties, null);

        // Program Setup
        String source = readFile("resources/Kernels/BarycentricTriangle.cl");

        // Create the program
        cl_program cpProgram = clCreateProgramWithSource(context, 1,
                new String[]{ source }, null, null);

        // Build the program
        clBuildProgram(cpProgram, 0, null, "-cl-mad-enable", null, null);

        // Create the kernel
        kernel = clCreateKernel(cpProgram, "drawTriangle", null);

        pixelMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                screenWidth * screenHeight * Sizeof.cl_uint, null, null);

        debugBufferMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                debugBuffer.length * Sizeof.cl_double, null, null);

        screenSizeMem = clCreateBuffer(context, CL_MEM_READ_ONLY,
                2 * Sizeof.cl_int, null, null);
        clEnqueueWriteBuffer(commandQueue, screenSizeMem, true, 0,
                2 * Sizeof.cl_int, Pointer.to(new int[]{screenWidth, screenHeight}), 0, null, null);

        for (int i=0;i<triangleArgs.length;i++)
        {
            triangleArgs[i] = clCreateBuffer(context, CL_MEM_READ_ONLY,
                    3 * Sizeof.cl_double, null, null);
        }
    }

    private void recreateDebugMem()
    {
        clReleaseMemObject(debugBufferMem);
        debugBufferMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                debugBuffer.length * Sizeof.cl_double, null, null);
    }

    private void recreatePixelMem()
    {
        clReleaseMemObject(pixelMem);
        pixelMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                screenWidth * screenHeight * Sizeof.cl_uint, null, null);
    }

    /**
     * Helper function which reads the file with the given name and returns
     * the contents of this file as a String. Will exit the application
     * if the file can not be read.
     *
     * @param fileName The name of the file to read.
     * @return The contents of the file
     */
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

    /**
     * Attach the mouse- and mouse wheel listeners to the glComponent
     * which allow zooming and panning the fractal
     */
    private void initInteraction()
    {
        imageComponent.addMouseWheelListener(new MouseWheelListener()
        {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e)
            {
                int dir = e.getWheelRotation();
                double[][] tran = MatrixHelper.setupTranslateMatrix(0, 0, 0);
                double[][] rotate = MatrixHelper.setupFullRotation(pitch+=pitchStep*dir, yaw+=yawStep*dir, roll += rollStep*dir);
                double[][] scale = new double[4][4];
                scale[0][0] = 1;
                scale[1][1] = 1;
                scale[2][2] = 1;
                scale[3][3] = 1;
                updateShape(tran, rotate, scale);
                updateImage();
            }
        });
    }

    private void initShape()
    {
        relativePoints = new double[][]{
                new double[]{0, 0, 0},
                new double[]{1, 0, 0},
                new double[]{1, 1, 0},
                new double[]{0, 1, 0},
                new double[]{0.5, 0.5, 0},
        };
        textureRelativePoints = new double[][] {
                new double[]{0, 1, 0},
                new double[]{1, 1, 0},
                new double[]{1, 0, 0},
                new double[]{0, 0, 0},
                new double[]{.5, .5, 0},
        };

        camera = new Camera(0, 0, 2);
        camera.lookAt(new double[] {0, 0, 0});
        camera.setup();

        vH = new ViewHandler((JPanel) imageComponent, camera, null);
        vH.updateMatrix();
    }

    private void updateShape()
    {
        double[][] tran = MatrixHelper.setupTranslateMatrix(0, 0, 0);
        double[][] rotate = MatrixHelper.setupFullRotation(0, 0, 0);
        double[][] scale = new double[4][4];
        scale[0][0] = 1;
        scale[1][1] = 1;
        scale[2][2] = 1;
        scale[3][3] = 1;
        updateShape(tran, rotate, scale);
    }

    private void updateShape(double[][] tran, double[][] rotate, double[][] scale)
    {
        Matrix combined = new Matrix(tran);
        combined = new Matrix(combined.matrixMultiply(rotate));
        combined = new Matrix(combined.matrixMultiply(scale));

        clipPoints = new double[relativePoints.length][];

        for (int i=0;i<relativePoints.length;i++)
        {
            clipPoints[i] = applyExplicitMatrix(combined, relativePoints[i]);
        }


        Matrix projectionMatrix = new Matrix(vH.getProjectionMatrix().matrixMultiply(camera.getMatrix()));
        System.out.println("Frusstum\n"+vH.getProjectionMatrix());
        System.out.println("Camera\n"+camera.getMatrix());
        System.out.println("Projection\n"+projectionMatrix);

        for (int i=0;i<clipPoints.length;i++)
        {
            clipPoints[i] = applyImplicitMatrix(projectionMatrix, clipPoints[i]);
        }
    }

    //NOTE: since pixel distribution happens on the triangle as a whole, when the triangle goes off screen, the distribution looks sparse as more of the triangle
    //is off screen than on
    //t: triangle first index is point, second is axis
    private long drawTriangle(double[][] t, double[][] textureAnchor)
    {
        //see barycentric test for explanation
        double len = (-t[1][0]*t[2][1]) - (t[0][0]*t[1][1]) + (t[0][0]*t[2][1]) + (t[1][0]*t[0][1]) + (t[2][0]*t[1][1]) - (t[2][0]*t[0][1]);
        len = Math.abs(screenHeight * screenWidth * len * .5);
        //added a maximum value so that we don't run out of memory
        len = Math.sqrt(len);

        double localLen = Math.ceil(Math.sqrt(len));

        //ensures that local length is the root of length
        len = localLen*localLen;

        localLen = Math.min(localLen, 32);
        len = Math.min(len, 1024);

        long globalWorkSize[];
        // Set work size and execute the kernel
        globalWorkSize = new long[]{
                (long)len, (long)len
        };

        long localWorkSize[];
        localWorkSize = new long[]{
                (long)localLen, (long)localLen
        };

        int sizeOfDebug = (int) ((len*len)/2);
        System.out.println("P "+sizeOfDebug);

        debugBuffer = new double[sizeOfDebug];
        recreateDebugMem();
        clSetKernelArg(kernel, 10, Sizeof.cl_mem, Pointer.to(debugBufferMem));

        setupTriangleArgs(t[0], t[1], t[2], textureAnchor[0], textureAnchor[1], textureAnchor[2]);
        cl_event event = new cl_event();

        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null,
                globalWorkSize, localWorkSize, 0, null, event);
        clWaitForEvents(1, new cl_event[]{event});

        long startTime[] = new long[1];
        long endTime[] = new long[1];
        clGetEventProfilingInfo(event, CL_PROFILING_COMMAND_END,
                Sizeof.cl_ulong, Pointer.to(endTime), null);
        clGetEventProfilingInfo(event, CL_PROFILING_COMMAND_START,
                Sizeof.cl_ulong, Pointer.to(startTime), null);

        return endTime[0]-startTime[0];
    }

    //this takes up to 8 times longer than the kernel, this is the bottleneck
    private void setupTriangleArgs(
            double[] t01, double[] t02, double[] t03,
            double[] tA01, double[] tA02, double[] tA03
    )
    {
        //set the triangle's points
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, setMemoryArg(0, t01));
        clSetKernelArg(kernel, 5, Sizeof.cl_mem, setMemoryArg(1, t02));
        clSetKernelArg(kernel, 6, Sizeof.cl_mem, setMemoryArg(2, t03));

//        set the texture map's points
        clSetKernelArg(kernel, 7, Sizeof.cl_mem, setMemoryArg(3, tA01));
        clSetKernelArg(kernel, 8, Sizeof.cl_mem, setMemoryArg(4, tA02));
        clSetKernelArg(kernel, 9, Sizeof.cl_mem, setMemoryArg(5, tA03));
    }

    private Pointer setMemoryArg(int index, double[] arr)
    {
        clReleaseMemObject(triangleArgs[index]);
        triangleArgs[index] = null;

        triangleArgs[index] = clCreateBuffer(context, CL_MEM_READ_ONLY,
                arr.length * Sizeof.cl_double, null, null);
        clEnqueueWriteBuffer(commandQueue, triangleArgs[index], true, 0,
                arr.length * Sizeof.cl_double, Pointer.to(arr), 0, null, null);

        return Pointer.to(triangleArgs[index]);
    }

    private long drawAllTriangles()
    {
        //green
        long timeTaken = drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[0],
                clipPoints[1],
        }, new double[][] {
                textureRelativePoints[4],
                textureRelativePoints[0],
                textureRelativePoints[1]});

        //blues
        timeTaken += drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[1],
                clipPoints[2],
        }, new double[][]{
                textureRelativePoints[4],
                textureRelativePoints[1],
                textureRelativePoints[2]});
        //red
        timeTaken += drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[2],
                clipPoints[3],
        }, new double[][]{
                textureRelativePoints[4],
                textureRelativePoints[2],
                textureRelativePoints[3]});
        //yellow
        timeTaken += drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[3],
                clipPoints[0],
        }, new double[][]{
                textureRelativePoints[4],
                textureRelativePoints[3],
                textureRelativePoints[0]});

        return timeTaken;
    }

    private void updateImage() {
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(pixelMem));

        long timeTaken = drawAllTriangles();
//        System.out.println("Took " + (timeTaken) +" ns");


        // Read the pixel data into the BufferedImage
        DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
        int data[] = dataBuffer.getData();

        clEnqueueReadBuffer(commandQueue, pixelMem, CL_TRUE, 0,
                Sizeof.cl_int * screenWidth*screenHeight, Pointer.to(data), 0, null, null);

        clEnqueueReadBuffer(commandQueue, debugBufferMem, CL_TRUE,0,
                Sizeof.cl_double * debugBuffer.length, Pointer.to(debugBuffer), 0, null, null);

        System.out.println("Debug Start");
        int count = 0;
        for(double d: debugBuffer)
        {
            if (d == 1) {
                count++;
            }
        }
        System.out.println(count);
        System.out.println("Debug End");

        recreatePixelMem();
        imageComponent.repaint();
    }

    private double[] applyImplicitMatrix(Matrix m, double[] d)
    {
        d = m.pointMultiply(d);
        return new double[]{
                d[0] /= d[3],
                d[1] /= d[3],
                d[2] /= d[3]
        };
    }

    private double[] applyExplicitMatrix(Matrix m, double[] d)
    {
        d = m.pointMultiply(d);
        double absP = Math.abs(d[3]);
        return new double[]{
                d[0] / absP,
                d[1] / absP,
                d[2] / absP,
        };
    }
}

