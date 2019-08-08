package TextureGraphics.Memory.Array;

import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_mem;

import static org.jocl.CL.clEnqueueWriteBuffer;

public class IntArrayData extends ArrayData {

    public int[] data;

    public IntArrayData(int[] data)
    {
        this.data = data;
        this.byteSize = data.length * Sizeof.cl_int;
    }

    public void writeToQueue(cl_command_queue commandQueue, cl_mem writeTo)
    {
        clEnqueueWriteBuffer(commandQueue, writeTo, true, offset,
                byteSize, Pointer.to(data), 0, null, null);
    }

}
