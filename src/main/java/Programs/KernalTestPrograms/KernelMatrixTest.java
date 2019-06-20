package Programs.KernalTestPrograms;

import GxEngine3D.Helper.Maths.MatrixHelper;
import GxEngine3D.Model.Matrix.Matrix;
import TextureGraphics.JoclProgram;
import org.jocl.Pointer;
import org.jocl.Sizeof;

import javax.swing.*;

import static org.jocl.CL.*;

public class KernelMatrixTest extends JoclProgram{
    public static void main(String args[])
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                new KernelMatrixTest();
            }
        });
    }

    public KernelMatrixTest()
    {
        create("resources/Kernels/KernelMatrix.cl", "applyMatrix");
        super.start();

        setup();
        setupArgs();

        long[] globalWorkSize = new long[]{
                (long)1
        };
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                globalWorkSize, null, 0, null, null);

        double[] out = new double[3];
        clEnqueueReadBuffer(commandQueue, dynamic.get("Output").getRawObject(), CL_TRUE, 0,
                Sizeof.cl_double * out.length, Pointer.to(out), 0, null, null);

        printResult("GPU Out", out);
    }

    private void printResult(String title, double[] result)
    {
        System.out.println(title);
        for (int i=0;i<result.length;i++)
        {
            System.out.println(result[i]);
        }
    }

    @Override
    public void setup() {
        super.setup();
        setupMemory();
    }

    private Matrix createMatrix()
    {
        double[][] rot = MatrixHelper.setupFullRotation(0, 0, 0);
        double[][] scale = MatrixHelper.setupIdentityMatrix();
        double[][] trans = MatrixHelper.setupTranslateMatrix(10, 2, -1);

        Matrix combined = new Matrix(trans);
        combined = new Matrix(combined.matrixMultiply(rot));
        return new Matrix(combined.matrixMultiply(scale));
    }

    private void setupMemory()
    {
        double[] matrixFlat = createMatrix().flatten(),
                point = new double[]{0, 0, 0};

        double[] sanity = createMatrix().pointMultiply(new double[]{0, 0, 0});
        sanity = new double[]{
                sanity[0] / sanity[3],
                sanity[1] / sanity[3],
                sanity[2] / sanity[3],
        };
        printResult("CPU Out", sanity);

        dynamic.put(null, "Matrix", matrixFlat, 0, CL_MEM_READ_ONLY);
        dynamic.put(null, "Point", point, 0, CL_MEM_READ_ONLY);

        dynamic.put("Output", 3 * Sizeof.cl_double, CL_MEM_WRITE_ONLY, true);
    }

    private void setupArgs()
    {
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, dynamic.get("Matrix").getObject());
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, dynamic.get("Point").getObject());

        clSetKernelArg(kernel, 2, Sizeof.cl_mem, dynamic.get("Output").getObject());
    }
}
