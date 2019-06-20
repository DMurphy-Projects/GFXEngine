package TextureGraphics.Memory;

import GxEngine3D.Helper.ValueBasedIdGen;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_event;

import java.nio.ByteBuffer;

public class CachedMemoryHandler extends MemoryHandler {

    public CachedMemoryHandler(cl_context context, cl_command_queue commandQueue) {
        super(context, commandQueue);
    }

    //async call
    @Override
    protected IJoclMemory putAsync(cl_event task, String name, ByteBuffer buffer, int offset, long type)
    {
        String id = ValueBasedIdGen.generate(buffer);
        if (names.containsKey(id))
        {
            return memory.get(names.get(id));
        }

        //treat it as a regular variable
        return super.putAsync(task, id, buffer, offset, type);
    }

    //blocking call
    @Override
    protected IJoclMemory putSync(String name, ByteBuffer buffer, int offset, long type)
    {
        String id = ValueBasedIdGen.generate(buffer);
        if (names.containsKey(id))
        {
            return memory.get(names.get(id));
        }

        return super.putSync(id, buffer, offset, type);
    }

    //non-write call
    @Override
    protected IJoclMemory putEmpty(String name, int totalSize, long type, cl_event taskEvent)
    {
        //since we cant create a id based on contents when there are none, throw exception
        if (name == null)
        {
            try {
                throw new CachedMemoryHandlerException();
            } catch (CachedMemoryHandlerException e) {
                e.printStackTrace();
            }
        }
        if (names.containsKey(name))
        {
            return memory.get(names.get(name));
        }

        return super.putEmpty(name, totalSize, type, taskEvent);
    }

    private class CachedMemoryHandlerException extends Exception
    {
        @Override
        public String toString() {
            return "CachedMemoryHandler: A name must be provided for non-write implementation";
        }
    }
}
