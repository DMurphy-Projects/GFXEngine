package TextureGraphics;

import GxEngine3D.Helper.PolygonSplitter;
import org.jocl.*;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import static org.jocl.CL.*;

public class BarycentricGpuRender extends JoclRenderer {

    int screenWidth, screenHeight;

    BufferedImage image;

    ArrayList<int[]> pixel;
    ArrayList<double[]> zMap, debug01, debug02;
    double[] zMapStart;

    private cl_mem screenSizeMem;

    private final boolean debug = false;

    public BarycentricGpuRender(int screenWidth, int screenHight)
    {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHight;

        create("resources/Kernels/BarycentricTriangleWithZIndex.cl", "drawTriangle");

        initMemoryVariables();
        setupScreenSizeArgs();

        zMapStart = new double[screenWidth*screenHight];
        Arrays.fill(zMapStart, 1);
    }

    private void initMemoryVariables()
    {
        inArrays = new cl_mem[6];
        outArrays = new cl_mem[2];
        if (debug)
        {
            outArrays = new cl_mem[outArrays.length+2];
        }

        screenSizeMem = clCreateBuffer(context, CL_MEM_READ_ONLY,
                2 * Sizeof.cl_int, null, null);
        clEnqueueWriteBuffer(commandQueue, screenSizeMem, true, 0,
                2 * Sizeof.cl_int, Pointer.to(new int[]{screenWidth, screenHeight}), 0, null, null);

        //init triangle args
        for (int i = 0; i< inArrays.length; i++)
        {
            inArrays[i] = clCreateBuffer(context, CL_MEM_READ_ONLY,
                    3 * Sizeof.cl_double, null, null);
        }
        //init out args
        for (int i = 0; i< outArrays.length; i++)
        {
            outArrays[i] = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                    1 * Sizeof.cl_double, null, null);
        }
    }

    public void setup()
    {
        image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);

        pixel = new ArrayList<>();
        zMap = new ArrayList<>();

        if (debug) {
            debug01 = new ArrayList<>();
            debug02 = new ArrayList<>();
        }
    }

    public void render(double[][] polygon, double[][] textureAnchor, JoclTexture texture)
    {
        setupTextureArgs(texture);

        //if not a triangle, the texture anchor should have the same number of points as the polygon
        if (polygon.length > 3) {
            //should be triangles now
            polygon = PolygonSplitter.splitPolygonMidPoint(polygon);
            textureAnchor = PolygonSplitter.splitPolygonMidPoint(textureAnchor);
        }

        recreateOutputMemory(screenWidth*screenHeight);
        setupOutArgs();

        if (debug) {
            recreateDebugOutputMemory(screenWidth * screenHeight);
            setupDebugOutArgs();
        }

        int prev = polygon.length-2;
        for (int i=0;i<polygon.length-1;i++)
        {
            renderTriangle(polygon[polygon.length-1],
                    polygon[prev],
                    polygon[i],
                    textureAnchor[polygon.length-1], textureAnchor[prev], textureAnchor[i]);
            prev = i;
        }
        readData();
    }

    private void renderTriangle(double[] p1, double[] p2, double[] p3, double[] t1, double[] t2, double[] t3)
    {
        double len = calcLength(p1, p2, p3);
        double localLen = Math.ceil(Math.sqrt(len));

        //ensures that local length is the root of length
        len = localLen*localLen;

        //added a maximum value so that we don't run out of memory
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


        setupTriangleArgs(p1, p2, p3, t1, t2, t3);

        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null,
                globalWorkSize, localWorkSize, 0, null, null);
    }

    public BufferedImage createImage()
    {
        DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
        int data[] = dataBuffer.getData();

        double[] zMap = new double[screenWidth * screenHeight];
        Arrays.fill(zMap, 1);

        for (int i=0;i<this.zMap.size();i++)
        {
            int[] pixel = this.pixel.get(i);
            double[] zValues = this.zMap.get(i);

            for (int ii=0;ii<zValues.length;ii++)
            {
                double zValue = zValues[ii];
                if (zValue < zMap[ii])
                {
                    data[ii] = pixel[ii];
                    zMap[ii] = zValue;
                }
            }
        }

        return image;
    }

    private double calcLength(double[] p1, double[] p2, double[] p3)
    {
        //see barycentric test for explanation
        double len = (-p2[0]*p3[1]) - (p1[0]*p2[1]) + (p1[0]*p3[1]) + (p2[0]*p1[1]) + (p3[0]*p2[1]) - (p3[0]*p1[1]);
        len = Math.abs(screenHeight * screenWidth * len * .5);
        return Math.sqrt(len);
    }

    public void printDebug()
    {
        for (int i=0;i<debug01.size();i++)
        {
            double[] d01 = debug01.get(i);
            double[] d02 = debug02.get(i);

            for (int ii=0;ii<d01.length;ii++)
            {
                double x = d01[ii];
                double y = d02[ii];
                if (x != 0 || y != 0)
                {
                    System.out.println(x+" "+y);
                }
            }
        }

    }

    //JOCL handling functions
    private void readData()
    {
        //read x values
        int[] out = new int[screenWidth * screenHeight];
        clEnqueueReadBuffer(commandQueue, outArrays[0], CL_TRUE, 0,
                Sizeof.cl_uint * screenWidth*screenHeight, Pointer.to(out), 0, null, null);
        pixel.add(out);

        //read z values
        double[] zOut = new double[screenWidth * screenHeight];
        clEnqueueReadBuffer(commandQueue, outArrays[1], CL_TRUE, 0,
                Sizeof.cl_double * screenWidth*screenHeight, Pointer.to(zOut), 0, null, null);
        zMap.add(zOut);

        if (debug) {
            zOut = new double[screenWidth * screenHeight];
            clEnqueueReadBuffer(commandQueue, outArrays[2], CL_TRUE, 0,
                    Sizeof.cl_double * screenWidth * screenHeight, Pointer.to(zOut), 0, null, null);
            debug01.add(zOut);

            zOut = new double[screenWidth * screenHeight];
            clEnqueueReadBuffer(commandQueue, outArrays[3], CL_TRUE, 0,
                    Sizeof.cl_double * screenWidth * screenHeight, Pointer.to(zOut), 0, null, null);
            debug02.add(zOut);
        }
    }

    private void recreateOutputMemory(int size)
    {
        setMemoryArgOut(0, size, Sizeof.cl_uint);
        setMemoryArgOut(1, size, Sizeof.cl_double);

        clEnqueueWriteBuffer(commandQueue, outArrays[1], true, 0,
                size * Sizeof.cl_double, Pointer.to(zMapStart), 0, null, null);
    }

    private void recreateDebugOutputMemory(int size)
    {
        setMemoryArgOut(2, size, Sizeof.cl_double);
        setMemoryArgOut(3, size, Sizeof.cl_double);
    }

    private void setupScreenSizeArgs()
    {
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(screenSizeMem));
    }
    private void setupTextureArgs(JoclTexture texture)
    {
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, texture.getTexture());
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, texture.getSize());
    }
    private void setupTriangleArgs(double[] t01, double[] t02, double[] t03,
            double[] tA01, double[] tA02, double[] tA03)
    {
        //set the triangle's points
        clSetKernelArg(kernel, 3, Sizeof.cl_mem, setMemoryArg(0, t01));
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, setMemoryArg(1, t02));
        clSetKernelArg(kernel, 5, Sizeof.cl_mem, setMemoryArg(2, t03));

//        set the texture map's points
        clSetKernelArg(kernel, 6, Sizeof.cl_mem, setMemoryArg(3, tA01));
        clSetKernelArg(kernel, 7, Sizeof.cl_mem, setMemoryArg(4, tA02));
        clSetKernelArg(kernel, 8, Sizeof.cl_mem, setMemoryArg(5, tA03));
    }

    private void setupOutArgs()
    {
        clSetKernelArg(kernel, 9, Sizeof.cl_mem, Pointer.to(outArrays[0]));
        clSetKernelArg(kernel, 10, Sizeof.cl_mem, Pointer.to(outArrays[1]));
    }

    private void setupDebugOutArgs()
    {
        clSetKernelArg(kernel, 11, Sizeof.cl_mem, Pointer.to(outArrays[2]));
        clSetKernelArg(kernel, 12, Sizeof.cl_mem, Pointer.to(outArrays[3]));
    }

    private Pointer setMemoryArg(int index, double[] arr)
    {
        clReleaseMemObject(inArrays[index]);
        inArrays[index] = null;

        inArrays[index] = clCreateBuffer(context, CL_MEM_READ_ONLY,
                arr.length * Sizeof.cl_double, null, null);
        clEnqueueWriteBuffer(commandQueue, inArrays[index], true, 0,
                arr.length * Sizeof.cl_double, Pointer.to(arr), 0, null, null);

        return Pointer.to(inArrays[index]);
    }

    private void setMemoryArgOut(int index, int size, int type)
    {
        clReleaseMemObject(outArrays[index]);
        outArrays[index] = null;

        outArrays[index] = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                size * type, null, null);
    }
}
