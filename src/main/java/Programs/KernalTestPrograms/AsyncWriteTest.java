package Programs.KernalTestPrograms;

import TextureGraphics.JoclProgram;
import TextureGraphics.Memory.AsyncJoclMemory;
import TextureGraphics.Memory.IJoclMemory;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_event;

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
        clEnqueueReadBuffer(commandQueue, dynamic.get("Output").getRawObject(), CL_TRUE, 0,
                Sizeof.cl_double * dataSize, Pointer.to(out), 0, null, null);


        System.out.println("GPU Out");
        for (int i=0;i<dataSize;i++)
        {
            System.out.println(out[i]);
        }
    }

    @Override
    public void setup() {
        super.setup();
        setupMemory();
    }

    private void setupMemory()
    {
        double[] data = createTestData(dataSize);
        IJoclMemory m = dynamic.put(new cl_event(), "Input", data, 0, CL_MEM_READ_ONLY);
        dynamic.put("Output",dataSize*Sizeof.cl_double, CL_MEM_WRITE_ONLY, true);

        cl_event writing = ((AsyncJoclMemory)m).getFinishedWritingEvent()[0];
        clWaitForEvents(1, new cl_event[]{writing});
    }

    private void setupArgs()
    {
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, dynamic.get("Input").getObject());
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, dynamic.get("Output").getObject());
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
