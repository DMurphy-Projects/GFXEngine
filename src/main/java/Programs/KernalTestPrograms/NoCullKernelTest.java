package Programs.KernalTestPrograms;

import GxEngine3D.Helper.Maths.TriangularValueHelper;
import TextureGraphics.ExecutionStatistics;
import TextureGraphics.JoclProgram;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_event;

import javax.swing.*;

import static org.jocl.CL.*;

public class NoCullKernelTest extends JoclProgram {

    public static void main(String args[])
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                new NoCullKernelTest();
            }
        });
    }

    //note:
    //optimisation of this method seems ambiguous as there is no guaranteed way of splitting up the items and it is unclear what is best
    //  small work groups or large work groups? large local have better performance in this test
    //  heavy work items or light work items? light work items have better performance in this test
    //  is the removal of culling better or worse?
    //      using 1 dim + bulking: marginally worse at small values, only gets worse with large values
    //      using 1 dim + bulking + only using method as a starting point then manually iterating from there: ????
    //          sqrt calls tied to work items, minimizing sqrts also minimises work items
    //      using 2 dim: substantially worse
    //      note: due to the use of sqrt, the no cull method will always be worse, so no further work is needed until sqrt call can be eliminated from the method

    //dataSize has to be a triangular value to draw the entire triangle
    int dataSize, n = 1000;

    public NoCullKernelTest()
    {
        this.profiling = true;
        create("resources/Kernels/NoCull.cl", "testNoCull");
        super.start();

        //this controls how much we are bulking the work items up by, ie each work item will do at most 'max' operations
        int[] pair = TriangularValueHelper.findClosestMultiPair(n, 10);
        dataSize = pair[0];

        setup();
        setupArgs();

        int xDim = pair[0] / pair[1];

        long[] globalWorkSize = new long[]{
                xDim, pair[1]
        };

        long[] localWorkSize = new long[]{
                findLocalWorkSize(xDim, 1024), 1
        };

        cl_event task = new cl_event();
        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null,
                globalWorkSize, localWorkSize, 0, null, task);

        double[] out1 = new double[dataSize];
        clEnqueueReadBuffer(commandQueue, dynamic.get("Output1").getRawObject(), CL_TRUE, 0,
                Sizeof.cl_double * dataSize, Pointer.to(out1), 0, null, null);

        double[] out2 = new double[dataSize];
        clEnqueueReadBuffer(commandQueue, dynamic.get("Output2").getRawObject(), CL_TRUE, 0,
                Sizeof.cl_double * dataSize, Pointer.to(out2), 0, null, null);

//        System.out.println("GPU Out");
//        for (int i=0;i<dataSize;i++)
//        {
//            System.out.println(i+": "+out1[i]+", "+out2[i]);
//        }

        if (profiling)
        {
            ExecutionStatistics es = new ExecutionStatistics();
            es.addEntry("No Cull Kernel", task, ExecutionStatistics.Formats.Nano);
            es.print();
        }
    }

    private int findLocalWorkSize(int globalSize, int max)
    {
        //find any value where (globalSize / value) < max
        int value = 1;
        int division = 1;
        for (int i=max;i>1;i--)
        {
            if (globalSize % i > 0) continue;
            int div;
            if ((div = (globalSize / i)) <= max)
            {
                //alternate division for value, which is better large local groups or small?
                if (i > value)
                {
                    value = i;
                    division = div;
                }
            }
        }
        System.out.println("Local: "+value);
        return value;
    }

    @Override
    public void setup() {
        super.setup();
        setupMemory();
    }

    private void setupMemory()
    {
        dynamic.put(null, "Output1",dataSize*Sizeof.cl_double, CL_MEM_WRITE_ONLY);
        dynamic.put(null, "Output2",dataSize*Sizeof.cl_double, CL_MEM_WRITE_ONLY);
    }

    private void setupArgs()
    {
        clSetKernelArg(kernel, 0, Sizeof.cl_int, Pointer.to(new int[]{dataSize}));
        clSetKernelArg(kernel, 1, Sizeof.cl_int, Pointer.to(new int[]{n}));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, dynamic.get("Output1").getObject());
        clSetKernelArg(kernel, 3, Sizeof.cl_mem, dynamic.get("Output2").getObject());
    }
}
