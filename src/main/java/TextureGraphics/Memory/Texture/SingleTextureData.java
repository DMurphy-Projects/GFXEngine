package TextureGraphics.Memory.Texture;

import TextureGraphics.JoclSetup;
import org.jocl.*;

import static org.jocl.CL.CL_MEM_READ_ONLY;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clEnqueueWriteBuffer;

public class SingleTextureData implements ITextureData {

    int[] data, info;

    cl_mem textureSizeMem;

    cl_context context;
    cl_command_queue commandQueue;

    public SingleTextureData(JoclSetup setup)
    {
        context = setup.getContext();
        commandQueue = setup.getCommandQueue();
    }

    public void create(int width, int height, int[] data)
    {
        this.data = data;
        this.info = new int[]{width, height};

        textureSizeMem = clCreateBuffer(context, CL_MEM_READ_ONLY,
                2 * Sizeof.cl_int, null, null);
        clEnqueueWriteBuffer(commandQueue, textureSizeMem, true, 0,
                2 * Sizeof.cl_int, Pointer.to(new int[]{width, height}), 0, null, null);
    }

    @Override
    public void update(int pos, int data, ITexture texture) {
        this.data[pos] = data;
    }

    @Override
    public int getDataAt(int pos, ITexture texture) {
        return this.data[pos];
    }

    @Override
    public int[] getTextureData() {
        return data;
    }

    @Override
    public int[] getTextureInfoData() {
        return info;
    }

    @Override
    public MemoryDataPackage getClTextureData() {
        int width = info[0];
        int height = info[1];

        cl_mem textureMem = clCreateBuffer(context, CL_MEM_READ_ONLY,
                width * height * Sizeof.cl_uint, null, null);
        clEnqueueWriteBuffer(commandQueue, textureMem, true, 0,
                width * height * Sizeof.cl_uint, Pointer.to(data), 0, null, null);

        return new MemoryDataPackage(textureMem, null);
    }

    @Override
    public Pointer getClTextureInfoData() {
        return Pointer.to(textureSizeMem);
    }
}
