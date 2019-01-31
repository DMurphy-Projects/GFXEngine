package Programs.KernalTestPrograms;

import GxEngine3D.Helper.TriangularValueHelper;
import TextureGraphics.JoclProgram;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_mem;

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
    //  small work groups or large work groups?
    //  heavy work items or light work items?
    //  is the removal of culling better or worse?

    //dataSize has to be a triangular value to draw the entire triangle
    int dataSize;

    public NoCullKernelTest()
    {
        create("resources/Kernels/NoCull.cl", "testNoCull");
        super.start();

        //this controls how much we are bulking the work items up by, ie each work item will do at most 10 operations
        int[] pair = TriangularValueHelper.findClosestMultiPair(200, 10);
        dataSize = pair[0];

        setup();
        setupArgs();

        //note: 1-2: odd, 3-4:even, ...
        long[] globalWorkSize = new long[]{
                (long)dataSize / pair[1]
        };

        long[] localWorkSize = new long[]{
                findLocalWorkSize(dataSize / pair[1], 1024)
        };
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                globalWorkSize, localWorkSize, 0, null, null);

        double[] out1 = new double[dataSize];
        clEnqueueReadBuffer(commandQueue, getDynamic("Output1").getRawObject(), CL_TRUE, 0,
                Sizeof.cl_double * dataSize, Pointer.to(out1), 0, null, null);

        double[] out2 = new double[dataSize];
        clEnqueueReadBuffer(commandQueue, getDynamic("Output2").getRawObject(), CL_TRUE, 0,
                Sizeof.cl_double * dataSize, Pointer.to(out2), 0, null, null);

//        System.out.println("GPU Out");
//        for (int i=0;i<dataSize;i++)
//        {
//            System.out.println(i+": "+out1[i]+", "+out2[i]);
//        }

    }

    private int findLocalWorkSize(int globalSize, int max)
    {
        //find any value where (globalSize / value) < max
        int value = 1;
        int division = 1;
        for (int i=max;i>0;i--)
        {
            if (globalSize % i > 0) continue;
            int div;
            if ((div = (globalSize / i)) < max)
            {
                //alternate division for value, which is better large local groups or small?
                if (div > division)
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
    protected void initStaticMemory() {
        staticMemory = new cl_mem[0];
    }

    @Override
    public void setup() {
        super.setup();
        setupMemory();
    }

    private void setupMemory()
    {
        setMemoryArg(dataSize*Sizeof.cl_double, CL_MEM_WRITE_ONLY, "Output1");
        setMemoryArg(dataSize*Sizeof.cl_double, CL_MEM_WRITE_ONLY, "Output2");
    }

    private void setupArgs()
    {
        clSetKernelArg(kernel, 0, Sizeof.cl_int, Pointer.to(new int[]{dataSize}));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, getDynamic("Output1").getObject());
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, getDynamic("Output2").getObject());
    }
}
