package TextureGraphics;

import GxEngine3D.Helper.PolygonSplitter;
import GxEngine3D.Model.Matrix.Matrix;
import TextureGraphics.Memory.AsyncJoclMemory;
import TextureGraphics.Memory.JoclMemory;
import TextureGraphics.Memory.JoclTexture;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_event;
import org.jocl.cl_mem;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

import static org.jocl.CL.*;

//this kernel program is based on previous iterations, includes:
//  z-ordering on gpu, needs testing since a lot of things have changed since construction
//  reduction of point bottleneck by using the matrix method
//      how this works is since clip space points move around a lot, we cannot cache these results and async writing only goes so far
//      solutions for this include bulking the data into one write to take advantage of the huge bandwidth available, however this would need a complete rework of the
//      rendering algorithm. At current time it is not apparent how to do this. OR moving the conversion from relative -> world space -> clip space onto the gpu.
//      since relative points do not move and can be the same over many different shapes/polygons, they can be cached to a high degree. Thus removing the bottleneck of
//      mass points but introduces a new bottleneck on the matrices used.
//      For instance: the view/projection matrix will only change once per rendering cycle, whereas the translation/scale/rotate matrices movements are unbounded
//      Solutions:
//          only update the gpu side memory at time of render, adds to the bottleneck, can be made async. effectiveness to be seen
//          have set intervals when we update the gpu side memory, have a sort of hot swapping of cl_mem arguments, ie use the one we've got until writing is finished then swap last minute
//              this could lead to non-smooth motion of objects based on gpu memory performance
//          cache a set of  pre calculated matrices that describe a certain motion, good for repetitive motions, locks motion to a set resolution
//          this may not even be an issue, if we target 30-60 fps, then we have 16 million nano to 33 million nano to perform all movement and all movements can be
//              written async at the same time. current write times for two fresh matrices async is 6-7 million nano.

public class BarycentricGpuRender_v2 extends JoclRenderer {

    int screenWidth, screenHeight;

    BufferedImage image;

    double[] zMapStart;

    //technically can read before all events can finish, but since the bottleneck is memory io, it is unlikely to happen in this implementation
    cl_event[] prevEvents = null;

    //names to retrieve arguments by
    String pixelOut = "Out1", zMapOut = "Out2", screenSize = "ScreenSize";

    public BarycentricGpuRender_v2(int screenWidth, int screenHeight)
    {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create("resources/Kernels/BarycentricTriangleWithMatrix.cl", "drawTriangle");

        super.start();

        zMapStart = new double[screenWidth*screenHeight];
        Arrays.fill(zMapStart, 1);
    }

    @Override
    protected void initStaticMemory() {
        staticMemory = new cl_mem[1];

        setStaticMemoryArg(0, new int[]{screenWidth, screenHeight}, screenSize);
        setupScreenSizeArgs();
    }

    @Override
    public void setup()
    {
        super.setup();
        image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
        setupOutputMemory();
    }

    public void setMatrix(Matrix i, Matrix e)
    {
        JoclMemory m1 = setCachedMemoryArg(null, i.flatten(), CL_MEM_READ_ONLY);
        JoclMemory m2 = setMemoryArg(null, e.flatten(), CL_MEM_READ_ONLY);//cannot be cached as the camera constantly moves

        setupMatrixArgs(m1, m2);

        cl_event[] events = new cl_event[]{
                ((AsyncJoclMemory)m1).getFinishedWritingEvent(),
                ((AsyncJoclMemory)m2).getFinishedWritingEvent()
        };

        clWaitForEvents(events.length, events);
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

    private cl_event renderTriangle(double[] p1, double[] p2, double[] p3, double[] t1, double[] t2, double[] t3)
    {
        //this calculates the density of samples required
        //in this implementation, the points are in relative space
        //this means that density will not change based on size on screen
        //TODO this should work like previous iterations in that polygon screen size is what density is based on
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

    //this is the reason for majority slowdown. hunch is that most of the "work" here is waiting for kernels to finish
    public BufferedImage createImage()
    {
        DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
        int data[] = dataBuffer.getData();

        clWaitForEvents(prevEvents.length, prevEvents);
        readData(data);

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
        clEnqueueReadBuffer(commandQueue, getDynamic(pixelOut).getRawObject(), CL_TRUE, 0,
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
        setMemoryArg(size * Sizeof.cl_int, CL_MEM_WRITE_ONLY, pixelOut);
        setMemoryArg(zMapStart, CL_MEM_READ_WRITE, zMapOut);
    }

    private void setupOutArgs()
    {
        clSetKernelArg(kernel, 9, Sizeof.cl_mem, getDynamic(pixelOut).getObject());
        clSetKernelArg(kernel, 10, Sizeof.cl_mem, getDynamic(zMapOut).getObject());
    }
    //OUTPUT ARGUMENT END

    private void setupMatrixArgs(JoclMemory iMatrix, JoclMemory eMatrix)
    {
        clSetKernelArg(kernel, 11, Sizeof.cl_mem, iMatrix.getObject());
        clSetKernelArg(kernel, 12, Sizeof.cl_mem, eMatrix.getObject());
    }


    private void setupScreenSizeArgs()
    {
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(getStatic(screenSize)));
    }
    private void setupTextureArgs(JoclTexture texture)
    {
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, texture.getTexture());
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, texture.getSize());
    }

    //QUESTION: is it that we use this method a lot or are 6 calls slower than 1 call
    private cl_event[] setupTriangleArgs(double[] t01, double[] t02, double[] t03,
            double[] tA01, double[] tA02, double[] tA03, cl_event task)
    {
        cl_event[] events = new cl_event[6];
        JoclMemory m;
        int index = 0;

        //set the triangle's points
        //this is approx 90% of this method
        m = setCachedMemoryArg(task, t01, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent();
        clSetKernelArg(kernel, 3, Sizeof.cl_mem, m.getObject());

        m = setCachedMemoryArg(task, t02, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent();
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, m.getObject());

        m = setCachedMemoryArg(task, t03, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent();
        clSetKernelArg(kernel, 5, Sizeof.cl_mem, m.getObject());

        //set the texture map's points
        //this is approx 10% of this method
        m = setCachedMemoryArg(task, tA01, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent();
        clSetKernelArg(kernel, 6, Sizeof.cl_mem, m.getObject());

        m = setCachedMemoryArg(task, tA02, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent();
        clSetKernelArg(kernel, 7, Sizeof.cl_mem, m.getObject());

        m = setCachedMemoryArg(task, tA03, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent();
        clSetKernelArg(kernel, 8, Sizeof.cl_mem, m.getObject());

        return events;
    }
}
