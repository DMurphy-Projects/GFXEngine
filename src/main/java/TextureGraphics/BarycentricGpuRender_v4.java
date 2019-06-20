package TextureGraphics;

import GxEngine3D.Helper.ArrayHelper;
import GxEngine3D.Helper.Maths.VectorCalc;
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
import static org.jocl.CL.CL_MEM_READ_WRITE;
import static org.jocl.CL.clSetKernelArg;

//could be optimised further
//does not have the sparse pixel problem of predecessors however this is not robust against the flaws in the projection matrix
public class BarycentricGpuRender_v4 extends JoclRenderer{

    int screenWidth, screenHeight;

    BufferedImage image;

    double[] zMapStart;

    ArrayList<cl_event> taskEvents;

    //names to retrieve arguments by
    String pixelOut = "Out1", zMapOut = "Out2", screenSize = "ScreenSize";

    //arg indices
    int screenSizeArg = 0,
            texureArg = 1,
            textureSizeArg = 2,
            noPolygonsArg =3,
            relativeSpaceStart = 4,//uses 3 indices
            clipSpaceStart = 7,//uses 3 indices
            screenSpaceStart = 10,//uses 3 indices
            textureAnchorStart = 13,//uses 2 indices
            outArg = 15,
            zMapArg =16,
            planeEqArg = 17,
            offsetArg = 18,
            inverseMatrixArg = 19,
            preCalcArg = 20
    ;

    public BarycentricGpuRender_v4(int screenWidth, int screenHeight)
    {
        this.profiling = true;

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        create("resources/Kernels/TextureRenderPolyBary.cl", "drawTriangle");

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

    double[] inverseMatrix;
    public void setInverseMatrix(double[] i)
    {
        inverseMatrix = i;
    }

    double[][] relativePoly;
    public void setRelativePoly(double[][] relativePoly)
    {
        this.relativePoly = relativePoly;
    }

    @Override
    public void render(double[][] polygon, double[][] textureAnchor, ITexture texture) {
        setupTextureArgs(texture);

        double[][] screenPoly = new double[polygon.length][polygon[0].length];
        for (int i=0;i<polygon.length;i++)
        {
            //change from clip-space to screen-space but preserve z value
            screenPoly[i][0] = (int) ((polygon[i][0] + 1) * 0.5 * screenWidth);
            screenPoly[i][1] = (int) ((1 - (polygon[i][1] + 1) * 0.5) * screenHeight);
            screenPoly[i][2] = polygon[i][2];
        }

        double[][] preComputeVec = preComputeVectors(relativePoly);
        double[] preComputeValues = preComputeValeus(preComputeVec, relativePoly.length-2);
        double[] preComputeVecFlat = ArrayHelper.flatten(preComputeVec);

        //calc bounds for triangle
        Polygon p = new Polygon();
        for (double[] point:screenPoly)
        {
            p.addPoint((int)point[0], (int) point[1]);
        }
        Rectangle r = p.getBounds();
        r = r.intersection(new Rectangle(0, 0, screenWidth, screenHeight));

        double[][] relativePoly = flip(this.relativePoly);
        double[][] clipPoly = flip(polygon);
        screenPoly = flip(screenPoly);
        double[][] tAnchor = flip(textureAnchor);

        cl_event task = renderPolygon(relativePoly, clipPoly, screenPoly, tAnchor, preComputeVecFlat, preComputeValues,
                new int[]{r.x, r.y}, Math.max(r.getWidth(), 1), Math.max(r.getHeight(), 1));
        taskEvents.add(task);
    }

    private double[][] preComputeVectors(double[][] poly)
    {
        double[][] vectors = new double[poly.length-1][];
        for (int i=0;i<poly.length-1;i++)
        {
            vectors[i] = VectorCalc.sub(poly[i+1], poly[0]);//all vectors towards poly[0], this is both (C-A, B-A)
        }

        return vectors;
    }

    private double[] preComputeValeus(double[][] vectors, int noTriangles)
    {
        int size = 4;
        double[] preCalc = new double[noTriangles * size];
        for (int i=0;i<noTriangles;i++)
        {
            preCalc[i*size] = VectorCalc.dot(vectors[i+1], vectors[i+1]);//dot00
            preCalc[i*size+1] = VectorCalc.dot(vectors[i+1], vectors[i]);//dot01
            preCalc[i*size+2] = VectorCalc.dot(vectors[i], vectors[i]);//dot11
            preCalc[i*size+3] = (preCalc[i*size] * preCalc[i*size+2]) - (preCalc[i*size+1] * preCalc[i*size+1]);//denominator
        }
        return preCalc;
    }

    private cl_event renderPolygon(double[][] relativePoly, double[][] clipPoly, double[][] screenPoly, double[][] textureAnchor, double[] preCalcVectors, double[] preCalcValues,
                                   int[] offset, double width, double height)
    {
        //(tX, tY, tZ) should have the same length
        clSetKernelArg(kernel, noPolygonsArg, Sizeof.cl_int, Pointer.to(new int[]{relativePoly[0].length}));

        cl_event taskEvent = new cl_event();
        cl_event[] relativeEvents = setupPolygonArgs(relativeSpaceStart, taskEvent, relativePoly[0], relativePoly[1], relativePoly[2]);
        cl_event[] clipEvents = setupPolygonArgs( clipSpaceStart, taskEvent, clipPoly[0], clipPoly[1], clipPoly[2]);
        cl_event[] screenEvents = setupPolygonArgs(screenSpaceStart, taskEvent, screenPoly[0], screenPoly[1], screenPoly[2]);
        cl_event[] textureAnchorEvents = setupPolygonArgs(textureAnchorStart, taskEvent, textureAnchor[0], textureAnchor[1]);
        cl_event[] inverseMatrixEvents = setupPolygonArgs(inverseMatrixArg, taskEvent, inverseMatrix);
        cl_event[] preCalcEvents = setupPolygonArgs(preCalcArg, taskEvent, preCalcVectors, preCalcValues);


        cl_event eqEvent = setupPlaneEqaution(screenPoly[0], screenPoly[1], screenPoly[2], taskEvent);
        cl_event offEvent = setupOffset(offset, taskEvent);

        int arrayPos = 0;
        cl_event[] waitingEvents = new cl_event[relativeEvents.length + clipEvents.length + screenEvents.length + textureAnchorEvents.length + inverseMatrixEvents.length + preCalcEvents.length + 2];
        waitingEvents[0] = eqEvent;
        waitingEvents[1] = offEvent;
        System.arraycopy(relativeEvents, 0, waitingEvents, arrayPos+=2, relativeEvents.length);//moves relativeEvents into waitingEvents
        System.arraycopy(clipEvents, 0, waitingEvents, arrayPos+=relativeEvents.length, clipEvents.length);//moves clipEvents into waitingEvents
        System.arraycopy(screenEvents, 0, waitingEvents, arrayPos+=clipEvents.length, screenEvents.length);//moves screenEvents into waitingEvents
        System.arraycopy(textureAnchorEvents, 0, waitingEvents, arrayPos+=screenEvents.length, textureAnchorEvents.length);//moves textureAnchor into waitingEvents
        System.arraycopy(inverseMatrixEvents, 0, waitingEvents, arrayPos+=textureAnchorEvents.length, inverseMatrixEvents.length);//moves inverseMatrix into waitingEvents
        System.arraycopy(preCalcEvents, 0, waitingEvents, arrayPos+=inverseMatrixEvents.length, preCalcEvents.length);//moves preCalcEvents into waitingEvents

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
        //a waitEvents size of 0 means that we culled all triangles from the render, so we don't need to read the data
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
//            stats.printTotal();
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
        dynamic.put(pixelOut, size * Sizeof.cl_int, CL_MEM_WRITE_ONLY, null);
        dynamic.put(null, zMapOut, zMapStart, 0, CL_MEM_READ_WRITE);
    }

    private void setupOutArgs()
    {
        clSetKernelArg(kernel, outArg, Sizeof.cl_mem, dynamic.get(pixelOut).getObject());
        clSetKernelArg(kernel, zMapArg, Sizeof.cl_mem, dynamic.get(zMapOut).getObject());
    }
    //OUTPUT ARGUMENT END

    private cl_event setupOffset(int[] offset, cl_event task)
    {
        IJoclMemory m = dynamic.put(task, null, offset, 0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, offsetArg, Sizeof.cl_mem, m.getObject());
        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }

    private cl_event setupPlaneEqaution(double[] tX, double[] tY, double[] tZ, cl_event task)
    {
        double[] v1 = new double[]{tX[1] - tX[0], tY[1] - tY[0], tZ[1] - tZ[0]};
        double[] v2 = new double[]{tX[2] - tX[0], tY[2] - tY[0], tZ[2] - tZ[0]};
        double[] normalVector = VectorCalc.cross(v1, v2);

        double[] planeEq = VectorCalc.plane_v3_pointForm(normalVector, new double[]{tX[0], tY[0], tZ[0]});

        IJoclMemory m = dynamic.put(task, null, planeEq, 0, CL_MEM_READ_ONLY);
        clSetKernelArg(kernel, planeEqArg, Sizeof.cl_mem, m.getObject());
        return ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
    }

    private void setupTextureArgs(ITexture texture)
    {
        clSetKernelArg(kernel, texureArg, Sizeof.cl_mem, texture.getTexture());
        clSetKernelArg(kernel, textureSizeArg, Sizeof.cl_mem, texture.getSize());
    }

    private cl_event[] setupPolygonArgs(int start, cl_event task, double[]... data)
    {
        cl_event[] events = new cl_event[data.length];
        IJoclMemory m;
        int index = 0;

        for (double[] d: data)
        {
            m = dynamic.put(task, null, d, 0, CL_MEM_READ_ONLY);
            events[index++] = ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
            clSetKernelArg(kernel, start++, Sizeof.cl_mem, m.getObject());
        }

        return events;
    }
}
