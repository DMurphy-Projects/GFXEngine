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
import java.util.ArrayList;
import java.util.Arrays;

import static org.jocl.CL.*;

//this implementation uses a method that instead of drawing each triangle/poly sequentially, enqueues a range for the screen
//and iterates over each triangle in the scene to determine which triangle the pixel would be in
public class GpuRendererIterateColor extends  JoclRenderer{

    int screenWidth, screenHeight;

    BufferedImage image;

    double[] zMapStart;

    //names to retrieve arguments by
    String pixelOut = "Out1", zMapOut = "Out2", screenSize = "ScreenSize";

    //arg indices
    int nArg = 0,
            screenSizeArg = 1,
            triangleArrayArg = 2,
            boundBoxArrayArg = 3,
            outArg = 4,
            zMapArg =5,
            planeEqArg = 6,
            planeEqInfoArg = 7,
            colorArrayArg = 8
                    ;

    //data arrays
    int[] triangleArray, boundBoxArray, planeEqInfoArray, colorArray;
    double[] planeEqArray;

    int size, tCount, eqCount;

    ArrayList<cl_event> taskEvents;

    public GpuRendererIterateColor(int screenWidth, int screenHeight)
    {
        this.profiling = true;

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create("resources/Kernels/IteratePlainColor.cl", "drawTriangle");

        super.start();

        zMapStart = new double[screenWidth*screenHeight];
        Arrays.fill(zMapStart, 1);

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

        setupOutputMemory();
    }

    public void prepare(ArrayList<double[][]> clipPolygons)
    {
        this.size = 0;
        this.tCount = 0;
        this.eqCount = 0;

        int triangleCount = 0;
        for (double[][] poly:clipPolygons)
        {
            if (PolygonClipBoundsChecker.shouldCull(poly)) {
                this.size += 1;
                triangleCount += poly.length - 2;//based on how we're iterating, count triangles == n-2, where n is number of points in poly
            }
        }

        //this is per triangle
        triangleArray = new int[triangleCount*6];//each element contains 2 points => 3 integers, XY
        boundBoxArray = new int[triangleCount*4];//each element contains 4 integers, [x, y, width, height]
        planeEqInfoArray = new int[triangleCount];//each element only has 1 integer
        colorArray = new int[triangleCount];//each element only has 1 integer, the color value

        //this is per polygon
        planeEqArray = new double[size*4];//each element contains 4 doubles, [a, b, c, d] see plane equations
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
        //do triangles
        RegularTriangleIterator it = new RegularTriangleIterator();
        it.iterate(screenPoly);

        while(it.hasNext())
        {
            int[] index = it.next();
            Polygon p = new Polygon();

            int offset = 0;
            for (int _i: index) {
                for (int i = 0; i < 2; i++) {
                    triangleArray[(tCount * 6) + offset] = (int) screenPoly[_i][i];
                    offset++;
                }
                p.addPoint((int) screenPoly[_i][0], (int) screenPoly[_i][1]);
            }

            Rectangle r = p.getBounds();

            boundBoxArray[(tCount * 4) + 0] = r.x;
            boundBoxArray[(tCount * 4) + 1] = r.y;
            boundBoxArray[(tCount * 4) + 2] = r.width;
            boundBoxArray[(tCount * 4) + 3] = r.height;

            planeEqInfoArray[tCount] = eqCount;

            colorArray[tCount] = color;

            tCount++;
        }

        //do plane equation
        double[] v1 = new double[]{
                screenPoly[1][0] - screenPoly[0][0],
                screenPoly[1][1] - screenPoly[0][1],
                screenPoly[1][2] - screenPoly[0][2]
        };
        double[] v2 = new double[]{
                screenPoly[2][0] - screenPoly[0][0],
                screenPoly[2][1] - screenPoly[0][1],
                screenPoly[2][2] - screenPoly[0][2]
        };
        double[] normalVector = VectorCalc.norm(VectorCalc.cross(v1, v2));

        double[] planeEq = VectorCalc.plane_v3_pointForm(normalVector,
                new double[]{
                    screenPoly[0][0],
                    screenPoly[0][1],
                    screenPoly[0][2]
                });
        planeEqArray[(eqCount * 4) + 0] = planeEq[0];
        planeEqArray[(eqCount * 4) + 1] = planeEq[1];
        planeEqArray[(eqCount * 4) + 2] = planeEq[2];
        planeEqArray[(eqCount * 4) + 3] = planeEq[3];

        //this method adds multiple triangles per run,
        // however polygons should be planar, so we only add 1 planeEq per run
        eqCount++;
    }

    private void enqueueTasks()
    {
        cl_event taskEvent = new cl_event();
        //write all data to device
        setupN();
        cl_event[] writingEvents = new cl_event[5];

        writingEvents[0] = setupTriangleArray(taskEvent);
        writingEvents[1] = setupBoundboxArray(taskEvent);
        writingEvents[2] = setupPlaneEquationArray(taskEvent);
        writingEvents[3] = setupPlaneEqautionInfoArray(taskEvent);
        writingEvents[4] = setupColorArray(taskEvent);

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

        if (taskEvents.size() > 0) {

            DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
            int data[] = dataBuffer.getData();
            cl_event[] events = new cl_event[taskEvents.size()];
            taskEvents.toArray(events);

            clWaitForEvents(events.length, events);
            readData(data);

            ExecutionStatistics stats = new ExecutionStatistics();
            for (cl_event event:taskEvents)
            {
                stats.addEntry("", event, ExecutionStatistics.Formats.Nano);
            }
            stats.printTotal();
        }

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
        dynamic.put(pixelOut, size * Sizeof.cl_int, CL_MEM_WRITE_ONLY, true);
        dynamic.put(null, zMapOut, zMapStart, 0, CL_MEM_READ_WRITE);
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

    private cl_event setupPlaneEquationArray(cl_event task)
    {
        IJoclMemory m = dynamic.put(task, null, planeEqArray, 0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, planeEqArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }

    private cl_event setupPlaneEqautionInfoArray(cl_event task)
    {
        IJoclMemory m = dynamic.put(task, null, planeEqInfoArray, 0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, planeEqInfoArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }

    private cl_event setupColorArray(cl_event task)
    {
        IJoclMemory m = dynamic.put(task, null, colorArray, 0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, colorArrayArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }
}
