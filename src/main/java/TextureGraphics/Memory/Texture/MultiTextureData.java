package TextureGraphics.Memory.Texture;

import TextureGraphics.JoclSetup;
import org.jocl.*;

import java.util.HashMap;

import static org.jocl.CL.*;
import static org.jocl.CL.CL_TRUE;

public class MultiTextureData implements ITextureData {

    private class MultiPartPackage
    {
        public int offset;
        public int[] data, info;
        public boolean needsUpdate = true;

        public MultiPartPackage(int[] data, int[] info, int offset)
        {
            this.offset = offset;
            this.data = data;
            this.info = info;
        }
    }

    int dataOffset = 0, infoOffset = 0;
    HashMap<ITexture, MultiPartPackage> textures = new HashMap<>();

    cl_mem textureDataArray, textureInfoArray;

    cl_context context;
    cl_command_queue commandQueue;

    boolean needsUpdate = false;

    public MultiTextureData(JoclSetup setup)
    {
        context = setup.getContext();
        commandQueue = setup.getCommandQueue();
    }

    public void init(int dataSize, int dataElements)
    {
        textureDataArray = clCreateBuffer(context, CL_MEM_READ_ONLY,
                dataSize * Sizeof.cl_int, null, null);

        textureInfoArray = clCreateBuffer(context, CL_MEM_READ_ONLY,
                3 * dataElements * Sizeof.cl_int, null, null);
    }

    @Override
    public void create(int width, int height, int[] data, ITexture texture) {

        MultiPartPackage _package = new MultiPartPackage(data, new int[]{width, height, dataOffset / Sizeof.cl_int}, dataOffset);
        dataOffset += width * height * Sizeof.cl_int;

        textures.put(texture, _package);

        needsUpdate = true;

        int size = 3 * Sizeof.cl_int;
        clEnqueueWriteBuffer(commandQueue, textureInfoArray, true, infoOffset,
                size, Pointer.to(_package.info), 0, null, null);
        infoOffset += size;
    }

    @Override
    public void update(int pos, int data, ITexture texture) {
        MultiPartPackage _package = textures.get(texture);

        _package.data[pos] = data;
        _package.needsUpdate = true;

        needsUpdate = true;
    }

    @Override
    public int getDataAt(int pos, ITexture texture) {
        MultiPartPackage _package = textures.get(texture);

        return _package.data[pos];
    }

    @Override
    public int[] getTextureData(ITexture texture) {
        MultiPartPackage _package = textures.get(texture);

        return _package.data;
    }

    @Override
    public int[] getTextureInfoData(ITexture texture) {
        MultiPartPackage _package = textures.get(texture);

        return _package.info;
    }

    @Override
    public MemoryDataPackage getClTextureData()
    {
        if (needsUpdate)
        {
            needsUpdate = false;

            for (MultiPartPackage _package: textures.values())
            {
                if (_package.needsUpdate)
                {
                    _package.needsUpdate = false;

                    clEnqueueWriteBuffer(commandQueue, textureDataArray, true, _package.offset,
                            _package.data.length * Sizeof.cl_int, Pointer.to(_package.data), 0, null, null);
                }
            }
        }
        return new MemoryDataPackage(textureDataArray, null);
    }

    @Override
    public Pointer getClTextureInfoData() {
        return Pointer.to(textureInfoArray);
    }
}
