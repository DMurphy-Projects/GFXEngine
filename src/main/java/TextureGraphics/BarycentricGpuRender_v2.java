package TextureGraphics;

import GxEngine3D.Helper.Iterator.RegularTriangleIterator;
import GxEngine3D.Helper.Iterator.ITriangleIterator;
import GxEngine3D.Helper.PolygonClipBoundsChecker;
import GxEngine3D.Model.Matrix.Matrix;
import TextureGraphics.Memory.AsyncJoclMemory;
import TextureGraphics.Memory.IJoclMemory;
import TextureGraphics.Memory.Texture.ITexture;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_event;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
    cl_event[] matrixEvents = null;
    ArrayList<cl_event> taskEvents;

    //these should be the same as we are using the fact that the indices can be shared across all iterators
    //if they are different they must use the same order
    ITriangleIterator clipIt = new RegularTriangleIterator(), textureAnchorIt = new RegularTriangleIterator(), polyIt = new RegularTriangleIterator();

    //names to retrieve arguments by
    String pixelOut = "Out1", zMapOut = "Out2", screenSize = "ScreenSize";

    boolean cullPolygon = false;

    public BarycentricGpuRender_v2(int screenWidth, int screenHeight)
    {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create("resources/Kernels/Barycentric/BarycentricTriangleWithMatrix.cl", "drawTriangle");

        super.start();

        zMapStart = new double[screenWidth*screenHeight];
        Arrays.fill(zMapStart, 1);
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
        taskEvents = new ArrayList<>();
        image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
        setupOutputMemory();
    }

    public void setMatrix(Matrix i, Matrix e)
    {
        IJoclMemory m1 = cached.put(null, null, i.flatten(), 0, CL_MEM_READ_ONLY);
        IJoclMemory m2 = dynamic.put(null, null, e.flatten(), 0, CL_MEM_READ_ONLY);//cannot be cached as the camera constantly moves

        setupMatrixArgs(m1, m2);

        matrixEvents = new cl_event[]{
                ((AsyncJoclMemory)m1).getFinishedWritingEvent()[0],
                ((AsyncJoclMemory)m2).getFinishedWritingEvent()[0]
        };
    }

    public void setClipPolygon(double[][] clipPolygon)
    {
        cullPolygon = PolygonClipBoundsChecker.shouldCull(clipPolygon);
        clipIt.iterate(clipPolygon);
    }


    public void render(double[][] polygon, double[][] textureAnchor, ITexture texture)
    {
        if (cullPolygon) return;
        setupTextureArgs(texture);

        //calculating the density based on clip space can lead to 0's, pre calculate the densities to find out which triangles we need to draw
        HashMap<int[], Double> preCalc = new HashMap<>();

        while(clipIt.hasNext())
        {
            int[] index = clipIt.next();
            double density = calcLength(clipIt.get(index[0]), clipIt.get(index[1]), clipIt.get(index[2]));
            if (density > 0)
            {
                preCalc.put(index, density);
            }
        }

        polyIt.iterate(polygon);
        textureAnchorIt.iterate(textureAnchor);
        for (Map.Entry<int[], Double> entry : preCalc.entrySet()) {
            int[] indices = entry.getKey();

            cl_event event = renderTriangle(
                    polyIt.get(indices[0]),
                    polyIt.get(indices[1]),
                    polyIt.get(indices[2]),
                    textureAnchorIt.get(indices[0]),
                    textureAnchorIt.get(indices[1]),
                    textureAnchorIt.get(indices[2]),
                    entry.getValue()
            );
            taskEvents.add(event);
        }
    }

    private cl_event renderTriangle(double[] p1, double[] p2, double[] p3, double[] t1, double[] t2, double[] t3, double len)
    {
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
        int waitingSize = writingEvents.length + matrixEvents.length;
        cl_event[] waitingEvents = new cl_event[waitingSize];
        System.arraycopy(writingEvents, 0, waitingEvents, 0, writingEvents.length);//moves writingEvents into waitingEvents
        System.arraycopy(matrixEvents, 0, waitingEvents, writingEvents.length, matrixEvents.length);//moves matrix events onto the end of writingEvents

        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null,
                globalWorkSize, localWorkSize, waitingEvents.length, waitingEvents, taskEvent);

        return taskEvent;
    }

    public BufferedImage createImage()
    {
        //a waitEvents size of 0 means that we culled all triangles from the render, so we don't need to read the data
        if (taskEvents.size() > 0) {

            DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
            int data[] = dataBuffer.getData();
            cl_event[] events = new cl_event[taskEvents.size()];
            taskEvents.toArray(events);

            clWaitForEvents(events.length, events);
            readData(data);
        }

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
        dynamic.put(null, pixelOut, size * Sizeof.cl_int, CL_MEM_WRITE_ONLY);
        dynamic.put(null, zMapOut, zMapStart, 0, CL_MEM_READ_WRITE);
    }

    private void setupOutArgs()
    {
        clSetKernelArg(kernel, 9, Sizeof.cl_mem, dynamic.get(pixelOut).getObject());
        clSetKernelArg(kernel, 10, Sizeof.cl_mem, dynamic.get(zMapOut).getObject());
    }
    //OUTPUT ARGUMENT END

    private void setupMatrixArgs(IJoclMemory iMatrix, IJoclMemory eMatrix)
    {
        clSetKernelArg(kernel, 11, Sizeof.cl_mem, iMatrix.getObject());
        clSetKernelArg(kernel, 12, Sizeof.cl_mem, eMatrix.getObject());
    }


    private void setupScreenSizeArgs()
    {
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, immutable.get(screenSize).getObject());
    }
    private void setupTextureArgs(ITexture texture)
    {
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, texture.getTexture());
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, texture.getSize());
    }

    //QUESTION: is it that we use this method a lot or are 6 calls slower than 1 call
    private cl_event[] setupTriangleArgs(double[] t01, double[] t02, double[] t03,
            double[] tA01, double[] tA02, double[] tA03, cl_event task)
    {
        cl_event[] events = new cl_event[6];
        IJoclMemory m;
        int index = 0;

        //set the triangle's points
        m = cached.put(task, null, t01, 0, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
        clSetKernelArg(kernel, 3, Sizeof.cl_mem, m.getObject());

        m = cached.put(task, null, t02, 0, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, m.getObject());

        m = cached.put(task, null, t03, 0, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
        clSetKernelArg(kernel, 5, Sizeof.cl_mem, m.getObject());

        //set the texture map's points
        m = cached.put(task, null, tA01, 0, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
        clSetKernelArg(kernel, 6, Sizeof.cl_mem, m.getObject());

        m = cached.put(task, null, tA02, 0, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
        clSetKernelArg(kernel, 7, Sizeof.cl_mem, m.getObject());

        m = cached.put(task, null, tA03, 0, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
        clSetKernelArg(kernel, 8, Sizeof.cl_mem, m.getObject());

        return events;
    }
}
