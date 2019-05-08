package Programs.KernalTestPrograms;

/*
 * JOCL - Java bindings for OpenCL
 *
 * Copyright 2009 Marco Hutter - http://www.jocl.org/
 */


import static org.jocl.CL.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

import javax.imageio.ImageIO;
import javax.swing.*;

import GxEngine3D.Camera.Camera;
import GxEngine3D.Helper.Maths.MatrixHelper;
import GxEngine3D.Helper.Maths.Vector2DCalc;
import GxEngine3D.Helper.Maths.VectorCalc;
import GxEngine3D.Model.Matrix.Matrix;
import GxEngine3D.View.ViewHandler;
import org.jocl.*;

public class JoclTest02
{
    public static void main(String args[])
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                new JoclTest02(500,500);
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
    private cl_mem[] triangleArgs = new cl_mem[6];

    int tWidth, tHeight;

    double[][] relativePoints, textureRelativePoints, clipPoints;
    double[] debugBuffer = new double[10];

    Camera camera;
    ViewHandler vH;

    double roll = 0, rollStep = 0;
    double yaw = 0, yawStep = 0.1;
    double pitch = 0, pitchStep = 0;

    public JoclTest02(int width, int height)
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

        // Create a command-queue for the selected device
        commandQueue =
                clCreateCommandQueue(context, device, 0, null);

        // Program Setup
        String source = readFile("resources/Kernels/TextureTriangle.cl");

        // Create the program
        cl_program cpProgram = clCreateProgramWithSource(context, 1,
                new String[]{ source }, null, null);

        // Build the program
        clBuildProgram(cpProgram, 0, null, "-cl-mad-enable", null, null);

        // Create the kernel
        kernel = clCreateKernel(cpProgram, "drawTriangle", null);

        recreatePixelMem();
        recreateDebugMem();

        screenSizeMem = clCreateBuffer(context, CL_MEM_READ_ONLY,
                2 * Sizeof.cl_int, null, null);
        clEnqueueWriteBuffer(commandQueue, screenSizeMem, true, 0,
                2 * Sizeof.cl_int, Pointer.to(new int[]{screenWidth, screenHeight}), 0, null, null);
    }

    private void recreateDebugMem()
    {
        debugBufferMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                debugBuffer.length * Sizeof.cl_double, null, null);
    }

    private void recreatePixelMem()
    {
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
                new double[]{0.5, 0.5, 0},
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

        for (int i=0;i<clipPoints.length;i++)
        {
            clipPoints[i] = applyImplicitMatrix(projectionMatrix, clipPoints[i]);
        }
    }

    private void drawTriangle(double[][] triangle, double[][] textureAnchor)
    {
        long globalWorkSize[] = new long[]{1};

        double[] v01 = VectorCalc.sub(triangle[0], triangle[1]);
        double heightV01 = Math.abs(v01[1] * screenHeight);

        double[] v02 = VectorCalc.sub(triangle[0], triangle[2]);
        double heightV02 = Math.abs(v02[1] * screenHeight);

        double[] v03 = VectorCalc.sub(triangle[2], triangle[1]);

        if (Math.signum(v01[1]) != Math.signum(v02[1])) {
            //this fragment handles when the other two points are both above and below the origin
            //instead of coming from the centre going out, we start at the ends moving in
            double[] t01 = VectorCalc.sub(textureAnchor[1], textureAnchor[0]);//reversed
            double[] t02 = VectorCalc.sub(textureAnchor[2], textureAnchor[0]);//reversed
            double[] t03 = VectorCalc.sub(textureAnchor[2], textureAnchor[1]);

            double[] v04 = VectorCalc.sub(triangle[2], triangle[0]);

            double side = Vector2DCalc.side(triangle[2], triangle[1], triangle[0]) * -v04[1];

            setupTriangleArgs(textureAnchor[2], t02, t03, triangle[2], v04, triangle[1], side);
            clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                    globalWorkSize, null, 0, null, null);

            v04 = VectorCalc.sub(triangle[1], triangle[0]);
            side = Vector2DCalc.side(triangle[1], triangle[2], triangle[0]) * -v04[1];

            setupTriangleArgs(textureAnchor[1], t01, VectorCalc.mul_v_d(t03, -1), triangle[1], v04, triangle[2], side);
            clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                    globalWorkSize, null, 0, null, null);

        }
        else if (heightV01 > heightV02) {
            //this fragment handles whens both points are entirely above/below the origin, but the first traveling vector is longer
            //we should travel along the shortest edge first then pivot and do the rest of the longer edge
            double[] t01 = VectorCalc.sub(textureAnchor[0], textureAnchor[1]);
            double[] t02 = VectorCalc.sub(textureAnchor[0], textureAnchor[2]);

            double side = Vector2DCalc.side(triangle[1], triangle[2], triangle[0]) * -v02[1];

            setupTriangleArgs(textureAnchor[0], t02, t01, triangle[0], v02, triangle[1], side);
            clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                    globalWorkSize, null, 0, null, null);

            //if the heights are the same then we've already went over this portion
            if (heightV01 != heightV02) {
                v03 = VectorCalc.mul_v_d(v03, -1);

                double[] t03 = VectorCalc.sub(textureAnchor[1], textureAnchor[2]);
                double[] t04 = VectorCalc.sub(textureAnchor[1], textureAnchor[0]);

                side = Vector2DCalc.side(triangle[2], triangle[1], triangle[0]) * -v03[1];

                setupTriangleArgs(textureAnchor[1], t03, t04, triangle[1], v03, triangle[0], side);
                clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                        globalWorkSize, null, 0, null, null);
            }
        }
        else {
            //this fragment handles when both points are entirely above/below the origin
            double[] t01 = VectorCalc.sub(textureAnchor[0], textureAnchor[1]);
            double[] t02 = VectorCalc.sub(textureAnchor[0], textureAnchor[2]);
            double[] t03 = VectorCalc.sub(textureAnchor[2], textureAnchor[1]);

            double side = Vector2DCalc.side(triangle[2], triangle[1], triangle[0]) * -v01[1];

            setupTriangleArgs(textureAnchor[0], t01, t02, triangle[0], v01, triangle[2], side);
            clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                    globalWorkSize, null, 0, null, null);
            //if the heights are the same then we've already went over this portion
            if (heightV01 != heightV02) {
                side = Vector2DCalc.side(triangle[1], triangle[2], triangle[0]) * -v03[1];

                setupTriangleArgs(textureAnchor[2], t03, VectorCalc.mul_v_d(t02, -1), triangle[2], v03, triangle[0], side);
                clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                        globalWorkSize, null, 0, null, null);
            }
        }
    }

    private void setupTriangleArgs(
            double[] textureOrigin, double[] textureTravelVector01, double[] textureTravelVector02,
            double[] triangleOrigin, double[] triangleTravelVector, double[] otherTrianglePoint,
            double side
    )
    {
        //set texture origin
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, setMemoryArg(0, textureOrigin));
        //set texture travel vector 01
        clSetKernelArg(kernel, 5, Sizeof.cl_mem, setMemoryArg(1, textureTravelVector01));
        //set texture travel vector 02
        clSetKernelArg(kernel, 6, Sizeof.cl_mem, setMemoryArg(2, textureTravelVector02));
        //set triangle origin
        clSetKernelArg(kernel, 7, Sizeof.cl_mem, setMemoryArg(3, triangleOrigin));
        //set triangle travel vector
        clSetKernelArg(kernel, 8, Sizeof.cl_mem, setMemoryArg(4, triangleTravelVector));
        //set point that is not a part of the travel vector
        clSetKernelArg(kernel, 9, Sizeof.cl_mem, setMemoryArg(5, otherTrianglePoint));
        //set direction value
        clSetKernelArg(kernel, 10, Sizeof.cl_double, Pointer.to(new double[]{side}));
    }

    private Pointer setMemoryArg(int index, double[] arr)
    {
        triangleArgs[index] = clCreateBuffer(context, CL_MEM_READ_ONLY,
                arr.length * Sizeof.cl_double, null, null);

        clEnqueueWriteBuffer(commandQueue, triangleArgs[index], true, 0,
                arr.length * Sizeof.cl_double, Pointer.to(arr), 0, null, null);
        return Pointer.to(triangleArgs[index]);
    }

    private void drawAllTriangles()
    {
        //green
        drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[0],
                clipPoints[1],
        }, new double[][] {
                textureRelativePoints[4],
                textureRelativePoints[0],
                textureRelativePoints[1]});

        //blues
        drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[1],
                clipPoints[2],
        }, new double[][]{
                textureRelativePoints[4],
                textureRelativePoints[1],
                textureRelativePoints[2]});
        //red
        drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[2],
                clipPoints[3],
        }, new double[][]{
                textureRelativePoints[4],
                textureRelativePoints[2],
                textureRelativePoints[3]});
        //yellow
        drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[3],
                clipPoints[0],
        }, new double[][]{
                textureRelativePoints[4],
                textureRelativePoints[3],
                textureRelativePoints[0]});
    }

    private void updateImage() {
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(pixelMem));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(screenSizeMem));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(textureMem));
        clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(textureSizeMem));
        clSetKernelArg(kernel, 11, Sizeof.cl_mem, Pointer.to(debugBufferMem));

        long start = System.nanoTime();
        drawAllTriangles();
        long end = System.nanoTime();
        System.out.println("Took " + (end-start) +" ns");

        // Read the pixel data into the BufferedImage
        DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
        int data[] = dataBuffer.getData();

        clEnqueueReadBuffer(commandQueue, pixelMem, CL_TRUE, 0,
                Sizeof.cl_int * screenWidth*screenHeight, Pointer.to(data), 0, null, null);

//        clEnqueueReadBuffer(commandQueue, debugBufferMem, CL_TRUE,0,
//                Sizeof.cl_double * debugBuffer.length, Pointer.to(debugBuffer), 0, null, null);
//
//        System.out.println("Debug Start");
//        for(double d: debugBuffer)
//        {
//            System.out.println(d);
//        }
//        System.out.println("Debug End");

        recreatePixelMem();
        recreateDebugMem();
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

