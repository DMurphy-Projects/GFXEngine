package TextureGraphics.Memory;

import TextureGraphics.JoclSetup;

import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_mem;

import java.nio.ByteBuffer;

import static org.jocl.CL.CL_MEM_READ_WRITE;
import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.clEnqueueReadBuffer;

public class PixelOutputHandler {

    private cl_mem memory;

    private int size;

    private JoclSetup setup;

    public PixelOutputHandler(JoclSetup setup)
    {
        this.setup = setup;
    }

    public void setup(int width, int height)
    {
        int[] data = new int[width*height];

        this.size = width*height;
        ByteBuffer buffer = BufferHelper.createBuffer(data);

        memory = JoclMemoryMethods.write(setup.getContext(), setup.getCommandQueue(), buffer, CL_MEM_READ_WRITE);
    }

    public void read(int[] out)
    {
        clEnqueueReadBuffer(setup.getCommandQueue(), memory, CL_TRUE, 0,
                Sizeof.cl_uint * size, Pointer.to(out), 0, null, null);
    }

    public Pointer getPixelOut()
    {
        return Pointer.to(memory);
    }
}
