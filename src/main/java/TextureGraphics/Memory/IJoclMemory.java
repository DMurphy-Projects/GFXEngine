package TextureGraphics.Memory;

import org.jocl.*;

import java.nio.ByteBuffer;

public interface IJoclMemory {

    //create with data
    void create(cl_context context, cl_command_queue commandQueue, cl_event tF, ByteBuffer buffer, long type);
    //create without data
    void create(cl_context context, int size, long type);

    void write(cl_command_queue commandQueue, ByteBuffer buffer, int offset);

    void release();
    Pointer getObject();
    cl_mem getRawObject();
}
