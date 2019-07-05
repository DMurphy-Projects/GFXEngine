package TextureGraphics.Memory.Texture;

import org.jocl.cl_event;
import org.jocl.cl_mem;

public class MemoryDataPackage
{
    public cl_mem data;
    public cl_event async;

    public MemoryDataPackage(cl_mem data, cl_event async)
    {
        this.data = data;
        this.async = async;
    }
}