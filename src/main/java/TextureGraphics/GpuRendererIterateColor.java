package TextureGraphics;

import GxEngine3D.Helper.Iterator.RegularTriangleIterator;
import GxEngine3D.Helper.Maths.VectorCalc;
import GxEngine3D.Helper.PolygonClipBoundsChecker;
import TextureGraphics.Memory.AsyncJoclMemory;
import TextureGraphics.Memory.IJoclMemory;
import TextureGraphics.Memory.Texture.ITexture;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_event;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import static org.jocl.CL.*;

//this implementation uses a method that instead of drawing each triangle/poly sequentially, enqueues a range for the screen
//and iterates over each triangle in the scene to determine which triangle the pixel would be in
public class GpuRendererIterateColor extends  JoclRenderer{

    int screenWidth, screenHeight;

    BufferedImage image;

    //names to retrieve arguments by
    String pixelOut = "Out1", zMapOut = "Out2", screenSize = "ScreenSize";

    //arg indices
    int nArg = 0,
            screenSizeArg = 1,
            triangleArrayArg = 2,
            boundBoxArrayArg = 3,
            outArg = 4,
            zMapArg = 5,
            colorArrayArg = 6
                    ;

    //data arrays
    int[] boundBoxArray, colorArray;
    double[] triangleArray;

    int size, tCount;

    ArrayList<cl_event> taskEvents;
    cl_event taskEvent;

    ByteBuffer zMapBuffer;

    public GpuRendererIterateColor(int screenWidth, int screenHeight)
    {
        this.profiling = true;

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create("resources/Kernels/IteratePlainColor.cl", "drawTriangle");

        super.start();

        double[] zMapStart = new double[screenWidth*screenHeight];
        Arrays.fill(zMapStart, 1);

        zMapBuffer = BufferHelper.createBuffer(zMapStart);

        image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
    }

    @Override
    protected void initStaticMemory() {
        super.initStaticMemory();

        immutable.put(null, screenSize, new int[]{screenWidth, screenHeight}, 0, CL_MEM_READ_ONLY);
        setupScreenSizeArgs();
    }

    @Override
    public void setup() {
        super.setup();
        taskEvents = new ArrayList<>();

        taskEvent = new cl_event();

        setupOutputMemory();
    }

    public void prepare(ArrayList<double[][]> clipPolygons)
    {
        this.size = 0;
        this.tCount = 0;

        int triangleCount = 0;
        for (double[][] poly:clipPolygons)
        {
            if (!PolygonClipBoundsChecker.shouldCull(poly)) {
                this.size += 1;
                triangleCount += poly.length - 2;//based on how we're iterating, count triangles == n-2, where n is number of points in poly
            }
        }
        if (size == 0) return;

        //this is per triangle
        triangleArray = new double[triangleCount*9];//each element contains 2 points => 3 integers, XY
        boundBoxArray = new int[triangleCount*4];//each element contains 4 integers, [x, y, width, height]
        colorArray = new int[triangleCount];//each element only has 1 integer, the color value
    }

    double[][] screenPoly;
    public void setScreenPoly(double[][] polygon)
    {
        screenPoly = polygon;
    }

    int color;
    public void setColor(int color)
    {
        this.color = color;
    }

    @Override
    public void render(double[][] polygon, double[][] textureAnchor, ITexture texture) {
        if (PolygonClipBoundsChecker.shouldCull(polygon)) return;
        //do triangles
        RegularTriangleIterator it = new RegularTriangleIterator();
        it.iterate(screenPoly);

        while(it.hasNext())
        {
            int[] index = it.next();
            Polygon p = new Polygon();

            int offset = 0;
            for (int _i: index) {
                int size = screenPoly[_i].length;
                System.arraycopy(screenPoly[_i], 0, triangleArray, (tCount * 9) + offset, size);
                offset += size;

                p.addPoint((int) screenPoly[_i][0], (int) screenPoly[_i][1]);
            }

            Rectangle r = p.getBounds();

            boundBoxArray[(tCount * 4) + 0] = r.x;
            boundBoxArray[(tCount * 4) + 1] = r.y;
            boundBoxArray[(tCount * 4) + 2] = r.width;
            boundBoxArray[(tCount * 4) + 3] = r.height;

            colorArray[tCount] = color;

            tCount++;
        }
    }

    private void enqueueTasks()
    {
        if (size == 0) return;
        //write all data to device
        setupN();
        cl_event[] writingEvents = new cl_event[4];

        writingEvents[0] = setupTriangleArray(taskEvent);
        writingEvents[1] = setupBoundboxArray(taskEvent);
        writingEvents[2] = setupColorArray(taskEvent);
        writingEvents[3] = ((AsyncJoclMemory)dynamic.get(zMapOut)).getFinishedWritingEvent()[0];

        //enqueue ranges
        long[] globalWorkSize = new long[] {
                (long) screenWidth,
                (long) screenHeight
        };

        //TODO needs to be a relationship with the screen dimensions, currently hard coded
        long[] localWorkSize = new long[] {
                (long) screenWidth / 30,
                (long) screenHeight / 30
        };

        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null,
                globalWorkSize, localWorkSize, writingEvents.length, writingEvents, taskEvent);

        taskEvents.add(taskEvent);
    }

    @Override
    public BufferedImage createImage() {
        enqueueTasks();

        DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
        int data[] = dataBuffer.getData();

        if (taskEvents.size() > 0) {

            cl_event[] events = new cl_event[taskEvents.size()];
            taskEvents.toArray(events);

            clWaitForEvents(events.length, events);

            ExecutionStatistics stats = new ExecutionStatistics();
            for (cl_event event:taskEvents)
            {
                stats.addEntry("", event, ExecutionStatistics.Formats.Nano);
            }
            stats.printTotal();
        }
        readData(data);

        finish();
        return image;
    }

    //JOCL handling functions
    private void readData(int[] out)
    {
        //read image
        clEnqueueReadBuffer(commandQueue, dynamic.get(pixelOut).getRawObject(), CL_TRUE, 0,
                Sizeof.cl_uint * screenWidth*screenHeight, Pointer.to(out), 0, null, null);
    }

    private void setupScreenSizeArgs()
    {
        clSetKernelArg(kernel, screenSizeArg, Sizeof.cl_mem, immutable.get(screenSize).getObject());
    }

    //OUTPUT ARGUMENTS
    private void setupOutputMemory()
    {
        recreateOutputMemory(screenWidth*screenHeight);
        setupOutArgs();
    }

    private void recreateOutputMemory(int size)
    {
        //this uses put empty which has no async behaviour
        dynamic.put(null, pixelOut, size * Sizeof.cl_int, CL_MEM_WRITE_ONLY);

        //TODO creating the double buffer for this is expensive
        dynamic.put(taskEvent, zMapOut, zMapBuffer, 0, CL_MEM_READ_WRITE);
    }

    private void setupOutArgs()
    {
        clSetKernelArg(kernel, outArg, Sizeof.cl_mem, dynamic.get(pixelOut).getObject());
        clSetKernelArg(kernel, zMapArg, Sizeof.cl_mem, dynamic.get(zMapOut).getObject());
    }
    //OUTPUT ARGUMENT END

    private void setupN()
    {
        clSetKernelArg(kernel, nArg, Sizeof.cl_int, Pointer.to(new int[]{tCount}));
    }

    private cl_event setupTriangleArray(cl_event task)
    {
        IJoclMemory m = dynamic.put(task, null, triangleArray, 0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, triangleArrayArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }

    private cl_event setupBoundboxArray(cl_event task)
    {
        IJoclMemory m = dynamic.put(task, null, boundBoxArray, 0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, boundBoxArrayArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }

    private cl_event setupColorArray(cl_event task)
    {
        IJoclMemory m = dynamic.put(task, null, colorArray, 0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, colorArrayArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }
}
