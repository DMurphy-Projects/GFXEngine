package TextureGraphics;

import GxEngine3D.Helper.Iterator.ITriangleIterator;
import GxEngine3D.Helper.Iterator.RegularTriangleIterator;
import GxEngine3D.Helper.PolygonClipBoundsChecker;
import GxEngine3D.Helper.VectorCalc;
import GxEngine3D.Model.Vector;
import TextureGraphics.Memory.AsyncJoclMemory;
import TextureGraphics.Memory.JoclMemory;
import TextureGraphics.Memory.JoclMemoryMethods;
import TextureGraphics.Memory.Texture.ITexture;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_event;
import org.jocl.cl_mem;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;

import static org.jocl.CL.*;
import static org.jocl.CL.CL_MEM_READ_ONLY;

public class SolidTriangleRender extends JoclRenderer {

    int screenWidth, screenHeight;

    BufferedImage image;

    double[] zMapStart;

    ArrayList<cl_event> taskEvents;

    //names to retrieve arguments by
    String pixelOut = "Out1", zMapOut = "Out2", screenSize = "ScreenSize";

    //arg indices
    int screenSizeArg = 0,
        colorArg = 1,
        noPolygonsArg = 2,
        triangleStartArg = 3,//uses 3 indices
        outArg = 6,
        zMapArg = 7,
        planeEqArg = 8,
        offsetArg = 9;

    int color = Color.WHITE.getRGB();

    public SolidTriangleRender(int screenWidth, int screenHeight)
    {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create("resources/Kernels/TriangleRender.cl", "drawTriangle");

        super.start();

        zMapStart = new double[screenWidth*screenHeight];
        Arrays.fill(zMapStart, 1);
    }

    @Override
    protected void initStaticMemory() {
        super.initStaticMemory();

        immutable.put(null, screenSize, new int[]{screenWidth, screenHeight}, CL_MEM_READ_ONLY);
        setupScreenSizeArgs();
    }

    @Override
    public void setup() {
        super.setup();
        taskEvents = new ArrayList<>();
        image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
        setupOutputMemory();
    }

    //converts from the form {{x, y, z}, ...} into {{x0, x1, ...}, {y0, y1, ...}, ...}
    private double[][] flip(double[][] array)
    {
        double[][] newArray = new double[array[0].length][array.length];

        for (int i=0;i<array.length;i++)
        {
            for (int ii=0;ii<array[0].length;ii++) {
                newArray[ii][i] = array[i][ii];
            }
        }

        return newArray;
    }

    public void setColor(int c)
    {
        color = c;
    }

    @Override
    public void render(double[][] polygon, double[][] textureAnchor, ITexture texture) {
        if (PolygonClipBoundsChecker.shouldCull(polygon)) return;

        double[][] iPoly = new double[polygon.length][polygon[0].length];
        for (int i=0;i<polygon.length;i++)
        {
            //change from clip-space to screen-space but preserve z value
            iPoly[i][0] = (int) ((polygon[i][0] + 1) * 0.5 * screenWidth);
            iPoly[i][1] = (int) ((1 - (polygon[i][1] + 1) * 0.5) * screenHeight);
            iPoly[i][2] = polygon[i][2];
        }

        //calc bounds for triangle
        Polygon p = new Polygon();
        for (double[] point:iPoly)
        {
            p.addPoint((int)point[0], (int) point[1]);
        }
        Rectangle r = p.getBounds();

        iPoly = flip(iPoly);

//        cpuTest(r.x, r.y, r.width, r.height, iPoly);

        cl_event task = renderPolygon(iPoly, new int[]{r.x, r.y}, Math.max(r.getWidth(), 1), Math.max(r.getHeight(), 1));
        taskEvents.add(task);
    }

    private void cpuTest(int xOffset, int yOffset, int width, int height, double[][] poly)
    {
        for (int _i=0;_i<width;_i++)
        {
            for (int _ii=0;_ii<height;_ii++)
            {
                int x = _i + xOffset;
                int y = _ii + yOffset;

                //http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
                boolean inside = false;
                for ( int i = 0, j = poly.length - 1 ; i < poly.length ; j = i++ )
                {
                    if ( ( poly[ i ][1] > y ) != ( poly[ j ][1] > y ) &&
                            x < ( poly[ j ][0] - poly[ i ][0] ) * ( y - poly[ i ][1] ) / ( poly[ j ][1] - poly[ i ][1] ) + poly[ i ][0] )
                    {
                        inside = !inside;
                    }
                }

                if (inside)
                {
                    image.setRGB(x, y, Color.BLUE.getRGB());
                }
            }
        }
    }

    private cl_event renderPolygon(double[][] polygon, int[] offset, double width, double height)
    {
        setupColorArg(color);

        cl_event taskEvent = new cl_event();
        cl_event[] writingEvents = setupTriangleArgs(polygon[0], polygon[1], polygon[2], taskEvent);

        cl_event eqEvent = setupPlaneEqaution(polygon[0], polygon[1], polygon[2], taskEvent);
        cl_event offEvent = setupOffset(offset, taskEvent);

        cl_event[] waitingEvents = new cl_event[writingEvents.length + 2];
        System.arraycopy(writingEvents, 0, waitingEvents, 0, writingEvents.length);//moves writingEvents into waitingEvents
        waitingEvents[writingEvents.length] = eqEvent;
        waitingEvents[writingEvents.length + 1] = offEvent;

        long[] globalWorkSize = new long[] {
                (long) width,
                (long) height
        };

        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null,
                globalWorkSize, null, waitingEvents.length, waitingEvents, taskEvent);

        return taskEvent;
    }

    @Override
    public BufferedImage createImage() {
        //a taskEvents size of 0 means that we culled all triangles from the render, so we don't need to read the data
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

    //JOCL handling functions
    private void readData(int[] out)
    {
        //read image
        clEnqueueReadBuffer(commandQueue, dynamic.get(pixelOut).getRawObject(), CL_TRUE, 0,
                Sizeof.cl_uint * screenWidth*screenHeight, Pointer.to(out), 0, null, null);


//        double[] map = new double[screenWidth*screenHeight];
//        clEnqueueReadBuffer(commandQueue, getDynamic(zMapOut).getRawObject(), CL_TRUE, 0,
//                Sizeof.cl_uint * screenWidth*screenHeight, Pointer.to(map), 0, null, null);
//
//        for (double d:map)
//        {
//            if (d != 1 && d != 0)
//            {
//                System.out.println(d);
//            }
//        }
    }

    private void setupColorArg(int color)
    {
        clSetKernelArg(kernel, colorArg, Sizeof.cl_int, Pointer.to(new int[]{ color }));
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
        dynamic.put(pixelOut, size * Sizeof.cl_int, CL_MEM_WRITE_ONLY);
        dynamic.put(null, zMapOut, zMapStart, CL_MEM_READ_WRITE);
    }

    private void setupOutArgs()
    {
        clSetKernelArg(kernel, outArg, Sizeof.cl_mem, dynamic.get(pixelOut).getObject());
        clSetKernelArg(kernel, zMapArg, Sizeof.cl_mem, dynamic.get(zMapOut).getObject());
    }
    //OUTPUT ARGUMENT END

    private cl_event setupOffset(int[] offset, cl_event task)
    {
        JoclMemory m = dynamic.put(task, null, offset, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, offsetArg, Sizeof.cl_mem, m.getObject());
        return ((AsyncJoclMemory)m).getFinishedWritingEvent();
    }

    private cl_event setupPlaneEqaution(double[] tX, double[] tY, double[] tZ, cl_event task)
    {
        double[] v1 = new double[]{tX[1] - tX[0], tY[1] - tY[0], tZ[1] - tZ[0]};
        double[] v2 = new double[]{tX[2] - tX[0], tY[2] - tY[0], tZ[2] - tZ[0]};
        double[] normalVector = VectorCalc.cross(v1, v2);

        double[] planeEq = VectorCalc.plane_v3_pointForm(normalVector, new double[]{tX[0], tY[0], tZ[0]});

        JoclMemory m = dynamic.put(task, null, planeEq, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, planeEqArg, Sizeof.cl_mem, m.getObject());
        return ((AsyncJoclMemory)m).getFinishedWritingEvent();
    }

    private cl_event[] setupTriangleArgs(double[] tX, double[] tY, double[] tZ, cl_event task)
    {
        cl_event[] events = new cl_event[3];
        JoclMemory m;
        int index = 0;

        //(tX, tY, tZ) should have the same length
        clSetKernelArg(kernel, noPolygonsArg, Sizeof.cl_int, Pointer.to(new int[]{tX.length}));

        //set the triangle's points
        m = dynamic.put(task, null, tX, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent();
        clSetKernelArg(kernel, triangleStartArg, Sizeof.cl_mem, m.getObject());

        m = dynamic.put(task, null, tY, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent();
        clSetKernelArg(kernel, triangleStartArg+1, Sizeof.cl_mem, m.getObject());

        m = dynamic.put(task, null, tZ, CL_MEM_READ_ONLY);
        events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent();
        clSetKernelArg(kernel, triangleStartArg+2, Sizeof.cl_mem, m.getObject());

        return events;
    }
}
