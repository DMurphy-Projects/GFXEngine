package Programs.KernalTestPrograms;

import TextureGraphics.JoclProgram;
import TextureGraphics.Memory.AsyncJoclMemory;
import TextureGraphics.Memory.JoclMemory;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_event;
import org.jocl.cl_mem;

import javax.swing.*;

import static org.jocl.CL.*;

public class AsyncWriteTest extends JoclProgram{

    public static void main(String args[])
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                new AsyncWriteTest();
            }
        });
    }

    int dataSize = 10;

    public AsyncWriteTest()
    {
        create("resources/Kernels/AsyncWriteTest.cl", "testWrite");

        super.start();

        setup();
        setupArgs();

        long[] globalWorkSize = new long[]{
            (long)dataSize
        };
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                globalWorkSize, null, 0, null, null);

        double[] out = new double[dataSize];
        clEnqueueReadBuffer(commandQueue, getDynamic("Output").getRawObject(), CL_TRUE, 0,
                Sizeof.cl_double * dataSize, Pointer.to(out), 0, null, null);


        System.out.println("GPU Out");
        for (int i=0;i<dataSize;i++)
        {
            System.out.println(out[i]);
        }
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
        double[] data = createTestData(dataSize);
        JoclMemory m = setMemoryArg(new cl_event(), data, CL_MEM_READ_ONLY, "Input");
        setMemoryArg(dataSize*Sizeof.cl_double, CL_MEM_WRITE_ONLY, "Output");

        cl_event writing = ((AsyncJoclMemory)m).getFinishedWritingEvent();
        clWaitForEvents(1, new cl_event[]{writing});
    }

    private void setupArgs()
    {
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, getDynamic("Input").getObject());
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, getDynamic("Output").getObject());
    }


    private double[] createTestData(int size)
    {
        double[] data = new double[size];
        int value = 0, step = 1;
        for (int i=0;i<size;i++)
        {
            data[i] = value;
            value += step;
        }
        return data;
    }



}
