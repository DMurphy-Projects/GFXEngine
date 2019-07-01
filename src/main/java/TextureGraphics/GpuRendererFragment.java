package TextureGraphics;

import GxEngine3D.Helper.PolygonClipBoundsChecker;
import TextureGraphics.Memory.AsyncJoclMemory;
import TextureGraphics.Memory.BufferHelper;
import TextureGraphics.Memory.IJoclMemory;
import org.jocl.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import static org.jocl.CL.*;

//this implementation is based on the GpuRendererIterateColorPolygon
//the handling of the polygon points will be external
//NOTE:
//slowest parts:
//      Prepare: to be expected, as it has to reinterpret from a double buffer and check bounds
//      Create : this is really just waiting for the kernel to finish
public class GpuRendererFragment extends  JoclProgram{

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
            colorArrayArg = 7,
            indexArrayArg = 8
                    ;

    //data arrays
    int[] boundBoxArray, colorArray, indexArray;
    ArrayList<double[][]>  screenPolygonsList;
    int[] color;
    ByteBuffer zMapBuffer, pixelBuffer;

    int size, polygonCount;

    ArrayList<cl_event> taskEvents;
    cl_event taskEvent;

    public GpuRendererFragment(int screenWidth, int screenHeight, JoclProgram externalProgram)
    {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create("resources/Kernels/IteratePlainColorPolygonSkip.cl", "drawTriangle", externalProgram);

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
        polygonCount = 0;

        setupOutputMemory();
    }

    public void prepare(DoubleBuffer clipPolygons, DoubleBuffer screenPolygons, int[] polygonStart)
    {
        this.size = 0;
        int startIndex = 0;
        int index = 0;

        screenPolygonsList = new ArrayList<>();

        ArrayList<Integer> indexArrayList = new ArrayList<>();
        ArrayList<Integer> colorArrayList = new ArrayList<>();

        while(clipPolygons.hasRemaining())
        {
            int lower = polygonStart[startIndex];
            int upper = polygonStart[startIndex+1];
            int size = (upper - lower) / 3;

            double[][] cPolygon = new double[size][3];
            double[][] sPolygon = new double[size][3];

            for (int i=0;i<size;i++)
            {
                for (int ii=0;ii<3;ii++)
                {
                    cPolygon[i][ii] = clipPolygons.get();
                    sPolygon[i][ii] = screenPolygons.get();
                }
            }

            if (!PolygonClipBoundsChecker.shouldCull(cPolygon)) {
                this.size++;

                screenPolygonsList.add(sPolygon);

                indexArrayList.add(index);

                colorArrayList.add(color[index % color.length]);
            }
            index++;
        }

        if (size == 0) return;

        boundBoxArray = new int[size*4];//each element contains 4 integers, [x, y, width, height]
        colorArray = new int[size];//each element only has 1 integer, the color value
        indexArray = new int[size];

        int i = 0;
        for (Integer _i: indexArrayList)
        {
            indexArray[i] = _i;
            i++;
        }

        i = 0;
        for (Integer _i: colorArrayList)
        {
            colorArray[i] = _i;
            i++;
        }
    }

    public void setColor(int[] color)
    {
        this.color = color;
    }

    public void setupRender()
    {
        for (double[][] polygon: screenPolygonsList) {
            Polygon p = new Polygon();
            for (double[] point : polygon) {
                p.addPoint((int) point[0], (int) point[1]);
            }

            Rectangle r = p.getBounds();
            boundBoxArray[(polygonCount * 4) + 0] = r.x;
            boundBoxArray[(polygonCount * 4) + 1] = r.y;
            boundBoxArray[(polygonCount * 4) + 2] = r.width;
            boundBoxArray[(polygonCount * 4) + 3] = r.height;

            polygonCount++;
        }
    }

    public void enqueueTasks(cl_event externalTask)
    {
        if (size == 0) return;

        //write all data to device
        setupN();
        cl_event[] writingEvents = new cl_event[5];

        writingEvents[0] = setupBoundboxArray(taskEvent);
        writingEvents[1] = setupColorArray(taskEvent);
        writingEvents[2] = setupZBuffer(taskEvent);
        writingEvents[3] = externalTask;
        writingEvents[4] = setupIndexArray(taskEvent);

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

    public BufferedImage createImage() {

        DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
        int data[] = dataBuffer.getData();

        if (taskEvents.size() > 0) {
            cl_event[] events = new cl_event[taskEvents.size()];
            taskEvents.toArray(events);

            clWaitForEvents(events.length, events);

            if (profiling) {
                ExecutionStatistics stats = new ExecutionStatistics();
                for (cl_event event : taskEvents) {
                    stats.addEntry("", event, ExecutionStatistics.Formats.Nano);
                }
                stats.printTotal();
            }
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

    //----------EXTERNAL
    public void setupPolygonArray(IJoclMemory m)
    {
        clSetKernelArg(kernel, polygonArrayArg, Sizeof.cl_mem, m.getObject());
    }

    public void setupPolygonStartArray(IJoclMemory m)
    {
        clSetKernelArg(kernel, polygonStartArrayArg, Sizeof.cl_mem, m.getObject());
    }
    //----------EXTERNAL

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

    private cl_event setupZBuffer(cl_event task)
    {
        IJoclMemory m = dynamic.put(task, zMapOut, zMapBuffer, 0, CL_MEM_READ_WRITE);
        clSetKernelArg(kernel, zMapArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }

    private cl_event setupIndexArray(cl_event task)
    {
        IJoclMemory m = dynamic.put(task, "", indexArray, 0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, indexArrayArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }
}
