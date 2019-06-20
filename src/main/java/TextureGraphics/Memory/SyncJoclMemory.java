package TextureGraphics.Memory;

import GxEngine3D.Helper.Iterator.BaseIterator;
import org.jocl.*;

import java.nio.ByteBuffer;

import static org.jocl.CL.clReleaseMemObject;

public class SyncJoclMemory extends BaseJoclMemory {

    public void create(cl_context context, cl_command_queue commandQueue, ByteBuffer buffer, long type) {
        memoryObject = JoclMemoryMethods.write(context, commandQueue, buffer, type);
    }

    public void write(cl_command_queue commandQueue, ByteBuffer buffer, int offset)
    {
        JoclMemoryMethods.write(commandQueue, buffer, offset, memoryObject);
    }
}
