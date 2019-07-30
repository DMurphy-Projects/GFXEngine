package Programs.KernalTestPrograms.Archive;

import TextureGraphics.ExecutionStatistics;
import TextureGraphics.JoclProgram;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_event;

import javax.swing.*;

import static org.jocl.CL.*;

public class CullKernelTest extends JoclProgram {

    public static void main(String args[])
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                new CullKernelTest();
            }
        });
    }

    int dataSize, n = 1000;

    public CullKernelTest()
    {
        this.profiling = true;
        create("resources/Kernels/Archive/NoCull.cl", "testCull");
        super.start();

        dataSize = n;

        setup();
        setupArgs();
        
        long[] globalWorkSize = new long[]{
                (long)n, (long)n
        };

        long[] localWorkSize = new long[]{
                findLocalWorkSize(dataSize, 1024), 1
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
            es.addEntry("Cull Kernel", task, ExecutionStatistics.Formats.Nano);
            es.print();
        }
    }

    private int findLocalWorkSize(int globalSize, int max) {
        //find any value where (globalSize / value) < max
        int value = 1;
        int division = 1;
        for (int i = max; i > 1; i--) {
            if (globalSize % i > 0) continue;
            int div;
            if ((div = (globalSize / i)) <= max) {
                //alternate division for value, which is better large local groups or small?
                if (i > value) {
                    value = i;
                    division = div;
                }
            }
        }
        System.out.println("Local: " + value);
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
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, dynamic.get("Output1").getObject());
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, dynamic.get("Output2").getObject());
    }
}
