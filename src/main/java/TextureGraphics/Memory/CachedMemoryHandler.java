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
    protected JoclMemory put(cl_event task, String name, ByteBuffer buffer, long type, boolean nameValid)
    {
        String id = ValueBasedIdGen.generate(buffer);
        if (names.containsKey(id))
        {
            return memory.get(names.get(id));
        }

        //treat it as a regular variable
        return super.put(task, id, buffer, type, true);
    }

    //blocking call
    @Override
    protected JoclMemory put(String name, ByteBuffer buffer, long type, boolean nameValid)
    {
        String id = ValueBasedIdGen.generate(buffer);
        if (names.containsKey(id))
        {
            return memory.get(names.get(id));
        }

        return super.put(id, buffer, type, nameValid);
    }

    //non-write call
    @Override
    protected JoclMemory put(String name, int totalSize, long type, boolean nameValid)
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

        return super.put(name, totalSize, type, nameValid);
    }

    private class CachedMemoryHandlerException extends Exception
    {
        @Override
        public String toString() {
            return "CachedMemoryHandler: A name must be provided for non-write implementation";
        }
    }
}
