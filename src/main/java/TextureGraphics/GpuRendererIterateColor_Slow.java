package TextureGraphics;

import GxEngine3D.Helper.Iterator.IndexIterator;
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
public class GpuRendererIterateColor_Slow extends  JoclRenderer{

    int screenWidth, screenHeight;

    BufferedImage image;

    double[] zMapStart;

    //names to retrieve arguments by
    String pixelOut = "Out1", zMapOut = "Out2", screenSize = "ScreenSize",
            triangleArray = "TriangleArray", boundBoxArray = "BoundBoxArray", planeEqArray = "PlaneEqaution",
            planeInfoArray = "PlaneInfo", colorArray = "ColorArray";

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

    int size, triangleCount, eqCount, triangleOffset, boundBoxOffset, planeEqOffset, planeInfoOffset, colorOffset;

    ArrayList<cl_event> waitEvents;
    cl_event taskEvent;

    public GpuRendererIterateColor_Slow(int screenWidth, int screenHeight)
    {
        this.profiling = true;

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create("resources/Kernels/IteratePlainColor_Slow.cl", "drawTriangle");

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
        waitEvents = new ArrayList<>();

        setupOutputMemory();
    }

    public void prepare(ArrayList<double[][]> clipPolygons)
    {
        taskEvent = new cl_event();

        size = 0;
        eqCount = 0;

        triangleOffset = 0;
        boundBoxOffset = 0;
        planeEqOffset = 0;
        planeInfoOffset = 0;
        colorOffset = 0;

        triangleCount = 0;
        for (double[][] poly:clipPolygons)
        {
            if (!PolygonClipBoundsChecker.shouldCull(poly)) {
                size += 1;
                triangleCount += poly.length - 2;//based on how we're iterating, count triangles == n-2, where n is number of points in poly
            }
        }
        //there is nothing to render
        if (size == 0) { return; }

        //this is per triangle
        //each element contains 2 points => 3 integers, XY
        dynamic.put(taskEvent, triangleArray, triangleCount*6*Sizeof.cl_int, CL_MEM_READ_ONLY);

        //each element contains 4 integers, [x, y, width, height]
        dynamic.put(taskEvent, boundBoxArray, triangleCount*4*Sizeof.cl_int, CL_MEM_READ_ONLY);

        //each element only has 1 integer
        dynamic.put(taskEvent, planeInfoArray, triangleCount*Sizeof.cl_int, CL_MEM_READ_ONLY);

        //each element only has 1 integer, the color value
        dynamic.put(taskEvent, colorArray, triangleCount*Sizeof.cl_int, CL_MEM_READ_ONLY);

        //this is per polygon
        //each element contains 4 doubles, [a, b, c, d] see plane equations
        dynamic.put(taskEvent, planeEqArray, size*4*Sizeof.cl_double, CL_MEM_READ_ONLY);
    }

    int[][] screenPoly;
    public void setScreenPoly(int[][] polygon)
    {
        screenPoly = polygon;
    }

    int color;
    public void setColor(int color)
    {
        this.color = color;
    }

    @Override
    public void render(double[][] clipPolygon, double[][] textureAnchor, ITexture texture) {
        //do triangles
        IndexIterator it = new IndexIterator();
        it.iterate(screenPoly.length);

        while(it.hasNext())
        {
            int[] index = it.next();
            Polygon p = new Polygon();

            int[] triangle = new int[6];
            int offset = 0;
            for (int _i: index) {
                System.arraycopy(screenPoly[_i], 0, triangle, offset, screenPoly[_i].length);
                offset += screenPoly[_i].length;

                p.addPoint(screenPoly[_i][0], screenPoly[_i][1]);
            }
            dynamic.put(taskEvent, triangleArray, triangle, triangleOffset, 0);
            triangleOffset += triangle.length * Sizeof.cl_int;

            Rectangle r = p.getBounds();
            int[] bounds = {
                    r.x,
                    r.y,
                    r.width,
                    r.height
            };
            dynamic.put(taskEvent, boundBoxArray, bounds, boundBoxOffset, 0);
            boundBoxOffset += bounds.length * Sizeof.cl_int;

            dynamic.put(taskEvent, planeInfoArray, new int[]{ eqCount }, planeInfoOffset, 0);
            planeInfoOffset += Sizeof.cl_int;

            dynamic.put(taskEvent, colorArray, new int[]{ color }, colorOffset, 0);
            colorOffset += Sizeof.cl_int;
        }

        //do plane equation
        double[] v1 = new double[]{
                screenPoly[1][0] - screenPoly[0][0],
                screenPoly[1][1] - screenPoly[0][1],
                clipPolygon[1][2] - clipPolygon[0][2]
        };
        double[] v2 = new double[]{
                screenPoly[2][0] - screenPoly[0][0],
                screenPoly[2][1] - screenPoly[0][1],
                clipPolygon[2][2] - clipPolygon[0][2]
        };
        double[] normalVector = VectorCalc.norm(VectorCalc.cross(v1, v2));

        double[] planeEq = VectorCalc.plane_v3_pointForm(normalVector,
                new double[]{
                    screenPoly[0][0],
                    screenPoly[0][1],
                    clipPolygon[0][2]
                });
        dynamic.put(taskEvent, planeEqArray, planeEq, planeEqOffset, 0);
        planeEqOffset += planeEq.length * Sizeof.cl_double;

        //this method adds multiple triangles per run,
        // however polygons should be planar, so we only add 1 planeEq per run
        eqCount++;
    }

    private void setupVariables()
    {
        if (size == 0) return;
        //write all data to device
        setupN();

        cl_event[] triangle = setupTriangleArray(taskEvent);
        cl_event[] bounds = setupBoundboxArray(taskEvent);
        cl_event[] eqaution = setupPlaneEquationArray(taskEvent);
        cl_event[] info = setupPlaneEqautionInfoArray(taskEvent);
        cl_event[] color = setupColorArray(taskEvent);
        int eventSize = triangle.length + bounds.length + eqaution.length + info.length + color.length;

        //combine events into one array
        cl_event[] writingEvents = new cl_event[eventSize];
        int offset = 0;
        System.arraycopy(triangle, 0, writingEvents, offset, triangle.length);
        offset += triangle.length;
        System.arraycopy(bounds, 0, writingEvents, offset, bounds.length);
        offset += bounds.length;
        System.arraycopy(eqaution, 0, writingEvents, offset, eqaution.length);
        offset += eqaution.length;
        System.arraycopy(info, 0, writingEvents, offset, info.length);
        offset += info.length;
        System.arraycopy(color, 0, writingEvents, offset, color.length);

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

        waitEvents.add(taskEvent);
    }

    @Override
    public BufferedImage createImage() {
        setupVariables();

        DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
        int data[] = dataBuffer.getData();

        if (waitEvents.size() > 0) {
            cl_event[] events = new cl_event[waitEvents.size()];
            waitEvents.toArray(events);

            clWaitForEvents(events.length, events);

            ExecutionStatistics stats = new ExecutionStatistics();
            for (cl_event event: waitEvents)
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
        dynamic.put(null, pixelOut, size * Sizeof.cl_int, CL_MEM_WRITE_ONLY);
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
        clSetKernelArg(kernel, nArg, Sizeof.cl_int, Pointer.to(new int[]{triangleCount}));
    }

    private cl_event[] setupTriangleArray(cl_event task)
    {
        IJoclMemory m = dynamic.get(triangleArray);
        clSetKernelArg(kernel, triangleArrayArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent();
    }

    private cl_event[] setupBoundboxArray(cl_event task)
    {
        IJoclMemory m = dynamic.get(boundBoxArray);
        clSetKernelArg(kernel, boundBoxArrayArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent();
    }

    private cl_event[] setupPlaneEquationArray(cl_event task)
    {
        IJoclMemory m = dynamic.get(planeEqArray);
        clSetKernelArg(kernel, planeEqArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent();
    }

    private cl_event[] setupPlaneEqautionInfoArray(cl_event task)
    {
        IJoclMemory m = dynamic.get(planeInfoArray);
        clSetKernelArg(kernel, planeEqInfoArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent();
    }

    private cl_event[] setupColorArray(cl_event task)
    {
        IJoclMemory m = dynamic.get(colorArray);
        clSetKernelArg(kernel, colorArrayArg, Sizeof.cl_mem, m.getObject());

        return ((AsyncJoclMemory)m).getFinishedWritingEvent();
    }
}
