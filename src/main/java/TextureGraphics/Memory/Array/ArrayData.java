package TextureGraphics.Memory.Array;

import org.jocl.cl_command_queue;
import org.jocl.cl_mem;

//base class for all array data types
public abstract class ArrayData {

    protected int offset, byteSize;

    public void setOffset(int offset)
    {
        this.offset = offset;
    }

    public abstract void writeToQueue(cl_command_queue commandQueue, cl_mem writeTo);
}
