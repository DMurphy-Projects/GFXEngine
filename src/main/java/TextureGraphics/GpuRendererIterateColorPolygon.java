package TextureGraphics;

import GxEngine3D.Helper.PolygonClipBoundsChecker;
import TextureGraphics.Memory.*;
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
public class GpuRendererIterateColorPolygon extends  JoclRenderer{

    int screenWidth, screenHeight;

    BufferedImage image;

    //names to retrieve arguments by
    String pixelOut = "PixelOut", zMapOut = "ZMap", screenSize = "ScreenSize";

    //arg indices
    int nArg = 0,
            screenSizeArg = 1,
            polygonArrayArg = 2,
            polygonStartArrayArg = 3,
            boundBoxArrayArg = 4,
            outArg = 5,
            zMapArg = 6,
            colorArrayArg = 7
                    ;

    //data arrays
    int[] boundBoxArray, colorArray, polygonStartArray;
    double[] polygonArray;

    int size;

    ArrayList<cl_event> taskEvents;
    cl_event taskEvent;

    ByteBuffer zMapBuffer, pixelBuffer;

    int polygonMemoryOffset, polygonCount;

    public GpuRendererIterateColorPolygon(int screenWidth, int screenHeight)
    {
        this.profiling = true;

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create("resources/Kernels/IterateMethods/IteratePlainColorPolygon.cl", "drawTriangle");

        super.start();

        double[] zMapStart = new double[screenWidth*screenHeight];
        Arrays.fill(zMapStart, 1);

        zMapBuffer = BufferHelper.createBuffer(zMapStart);

        int[] pixelStart = new int[screenWidth*screenHeight];
        pixelBuffer = BufferHelper.createBuffer(pixelStart);

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

        polygonMemoryOffset = 0;
        polygonCount = 0;

        setupOutputMemory();
    }

    public void prepare(ArrayList<double[][]> clipPolygons)
    {
        this.size = 0;
        int pointCount = 0;

        for (double[][] poly:clipPolygons)
        {
            if (!PolygonClipBoundsChecker.shouldCull(poly)) {
                this.size += 1;
                pointCount += poly.length;
            }
        }
        if (size == 0) return;

        //this is per triangle
        polygonArray = new double[pointCount * 3];//each point contains 3 components, [x, y, z]

        polygonStartArray = new int[size+1];//each point contains 3 components, [x, y, z]
        polygonStartArray[size] = pointCount * 3;

        boundBoxArray = new int[size*4];//each element contains 4 integers, [x, y, width, height]
        colorArray = new int[size];//each element only has 1 integer, the color value
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

        polygonStartArray[polygonCount] = polygonMemoryOffset;

        Polygon p = new Polygon();

        for (int i=0;i<polygon.length;i++)
        {
            int size = screenPoly[i].length;
            System.arraycopy(screenPoly[i], 0, polygonArray, polygonMemoryOffset, size);
            polygonMemoryOffset += size;

            p.addPoint((int)screenPoly[i][0], (int) screenPoly[i][1]);
        }

        Rectangle r = p.getBounds();
        boundBoxArray[(polygonCount * 4) + 0] = r.x;
        boundBoxArray[(polygonCount * 4) + 1] = r.y;
        boundBoxArray[(polygonCount * 4) + 2] = r.width;
        boundBoxArray[(polygonCount * 4) + 3] = r.height;

        colorArray[polygonCount] = color;

        polygonCount++;
    }

    private void enqueueTasks()
    {
        if (size == 0) return;

        //write all data to device
        setupN();
        cl_event[] writingEvents = new cl_event[5];

        writingEvents[0] = setupPolygonArray(taskEvent);
        writingEvents[1] = setupBoundboxArray(taskEvent);
        writingEvents[2] = setupColorArray(taskEvent);
        writingEvents[3] = setupPolygonStartArray(taskEvent);
        writingEvents[4] = setupZBuffer(taskEvent);

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
        IJoclMemory m = dynamic.put(null, pixelOut, pixelBuffer, 0, CL_MEM_WRITE_ONLY);
        clSetKernelArg(kernel, outArg, Sizeof.cl_mem, m.getObject());
    }
    //OUTPUT ARGUMENT END

    private void setupN()
    {
        clSetKernelArg(kernel, nArg, Sizeof.cl_int, Pointer.to(new int[]{size}));
    }

    private cl_event setupPolygonArray(cl_event task)
    {
        IJoclMemory m = dynamic.put(task, null, polygonArray, 0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, polygonArrayArg, Sizeof.cl_mem, m.getObject());

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

    private cl_event setupPolygonStartArray(cl_event task)
    {
        IJoclMemory m = dynamic.put(task, null, polygonStartArray, 0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, polygonStartArrayArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }

    private cl_event setupZBuffer(cl_event task)
    {
        IJoclMemory m = dynamic.put(task, zMapOut, zMapBuffer, 0, CL_MEM_READ_WRITE);
        clSetKernelArg(kernel, zMapArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }
}
