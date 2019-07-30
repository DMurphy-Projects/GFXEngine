package TextureGraphics.BarycentricRender;

import GxEngine3D.Helper.Maths.PolygonSplitter;
import TextureGraphics.Archive.JoclRenderer;
import TextureGraphics.JoclSetup;
import TextureGraphics.Memory.AsyncJoclMemory;
import TextureGraphics.Memory.IJoclMemory;
import TextureGraphics.Memory.Texture.ITexture;
import TextureGraphics.Memory.Texture.MemoryDataPackage;
import org.jocl.*;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

import static org.jocl.CL.*;

//this will execute a kernel with built in z-ordering instead of doing the ordering on cpu
//NOTE: this method is twice as fast as the original, suggests that cpu ordering is slow
//with this method, 100 textured squares can be rendered although performance is choppy
//also each polygon gets split up via the central point, thus creating more triangles to render, some performance gain at low vertex counts, see below
//  midpoint splitting: f(t) = v;
//  optimal splitting ; f(t) = v-2;
//      where t is number of triangles and v is vertices

//Things to test:
//  non-blocking on writes
//  is one large write faster than multiple small writes
//  objects who's points don't change can be cached, not to realistic on this implementation but for future works

public class BarycentricGpuRender extends JoclRenderer {

    int screenWidth, screenHeight;

    BufferedImage image;

    double[] zMapStart;

    //technically can read before all events can finish, but since the bottleneck is memory io, it is unlikely to happen in this implementation
    cl_event[] prevEvents = null;

    //names to retrieve arguments by
    String pixelOut = "Out1", zMapOut = "Out2", screenSize = "ScreenSize";

    cl_mem textureMemory;

    int[] pixelStart;

    public BarycentricGpuRender(int screenWidth, int screenHeight, JoclSetup setup)
    {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create("resources/Kernels/Barycentric/BarycentricTriangleWithZOrdering.cl", "drawTriangle", setup);

        super.start();

        zMapStart = new double[screenWidth*screenHeight];
        Arrays.fill(zMapStart, 1);

        pixelStart = new int[screenWidth*screenHeight];
    }

    @Override
    protected void initStaticMemory() {
        super.initStaticMemory();

        immutable.put(null, screenSize, new int[]{screenWidth, screenHeight}, 0, CL_MEM_READ_ONLY);
        setupScreenSizeArgs();
    }

    @Override
    public void setup()
    {
        super.setup();
        image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
        setupOutputMemory();
    }

    public void render(double[][] polygon, double[][] textureAnchor, ITexture texture)
    {
        setupTextureArgs(texture);

        //if not a triangle, the texture anchor should have the same number of points as the polygon
        if (polygon.length > 3) {
            //should be triangles now
            polygon = PolygonSplitter.splitPolygonMidPoint(polygon);
            textureAnchor = PolygonSplitter.splitPolygonMidPoint(textureAnchor);
        }

        cl_event[] events = new cl_event[polygon.length-1];

        int prev = polygon.length-2;
        for (int i=0;i<polygon.length-1;i++)
        {
            events[i] = renderTriangle(polygon[polygon.length-1],
                    polygon[prev],
                    polygon[i],
                    textureAnchor[polygon.length-1], textureAnchor[prev], textureAnchor[i]);
            prev = i;
        }

        this.prevEvents = events;
    }

    //A bottleneck is in here
    private cl_event renderTriangle(double[] p1, double[] p2, double[] p3, double[] t1, double[] t2, double[] t3)
    {
        double len = calcLength(p1, p2, p3);
        if (len == 0) return null;

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

        cl_event taskEvent = new cl_event();

        cl_event[] writingEvents = setupTriangleArgs(p1, p2, p3, t1, t2, t3, taskEvent);
//        cl_event[] waitingEvents = new cl_event[writingEvents.length + prevEvents.length];
//        System.arraycopy(writingEvents, 0, waitingEvents, 0, writingEvents.length);
//        System.arraycopy(prevEvents, 0, waitingEvents, writingEvents.length, prevEvents.length);

        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null,
                globalWorkSize, localWorkSize, writingEvents.length, writingEvents, taskEvent);

        return taskEvent;
    }

    public BufferedImage createImage()
    {
        DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
        int data[] = dataBuffer.getData();

        clWaitForEvents(prevEvents.length, prevEvents);
        readData(data);

        clReleaseMemObject(textureMemory);
        finish();
        return image;
    }

    private double calcLength(double[] p1, double[] p2, double[] p3)
    {
        //see barycentric test for explanation
        double len = (-p2[0]*p3[1]) - (p1[0]*p2[1]) + (p1[0]*p3[1]) + (p2[0]*p1[1]) + (p3[0]*p2[1]) - (p3[0]*p1[1]);
        len = Math.abs(screenHeight * screenWidth * len * .5);
        return Math.sqrt(len);
    }

    //JOCL handling functions
    private void readData(int[] out)
    {
        //read image
        clEnqueueReadBuffer(commandQueue, dynamic.get(pixelOut).getRawObject(), CL_TRUE, 0,
                Sizeof.cl_uint * screenWidth*screenHeight, Pointer.to(out), 0, null, null);
    }

    //OUTPUT ARGUMENTS
    private void setupOutputMemory()
    {
        recreateOutputMemory(screenWidth*screenHeight);
        setupOutArgs();
    }

    private void recreateOutputMemory(int size)
    {
        dynamic.put(null, pixelOut, pixelStart ,0 , CL_MEM_WRITE_ONLY);
        dynamic.put(null, zMapOut, zMapStart, 0, CL_MEM_READ_WRITE);
    }

    private void setupOutArgs()
    {
        clSetKernelArg(kernel, 9, Sizeof.cl_mem, dynamic.get(pixelOut).getObject());
        clSetKernelArg(kernel, 10, Sizeof.cl_mem, dynamic.get(zMapOut).getObject());
    }
    //OUTPUT ARGUMENT END


    private void setupScreenSizeArgs()
    {
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, immutable.get(screenSize).getObject());
    }
    private void setupTextureArgs(ITexture texture)
    {
        MemoryDataPackage _package = texture.getDataHandler().getClTextureData();
        textureMemory = _package.data;

        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(textureMemory));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, texture.getDataHandler().getClTextureInfoData());
    }

    //QUESTION: is it that we use this method a lot or are 6 calls slower than 1 call
    private cl_event[] setupTriangleArgs(double[] t01, double[] t02, double[] t03,
            double[] tA01, double[] tA02, double[] tA03, cl_event task)
    {
        cl_event[] events = new cl_event[6];
        IJoclMemory m;
        int index = 0;

        //set the triangle's points
        //this is approx 90% of this method
        m = dynamic.put(task, null, t01, 0, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
        clSetKernelArg(kernel, 3, Sizeof.cl_mem, m.getObject());

        m = dynamic.put(task, null, t02, 0, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, m.getObject());

        m = dynamic.put(task, null, t03, 0, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
        clSetKernelArg(kernel, 5, Sizeof.cl_mem, m.getObject());

        //set the texture map's points
        //this is approx 10% of this method
        m = dynamic.put(task, null, tA01, 0, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
        clSetKernelArg(kernel, 6, Sizeof.cl_mem, m.getObject());

        m = dynamic.put(task, null, tA02, 0, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
        clSetKernelArg(kernel, 7, Sizeof.cl_mem, m.getObject());

        m = dynamic.put(task, null, tA03, 0, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
        clSetKernelArg(kernel, 8, Sizeof.cl_mem, m.getObject());

        return events;
    }
}