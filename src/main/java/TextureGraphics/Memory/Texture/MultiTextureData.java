package TextureGraphics.Memory.Texture;

import TextureGraphics.JoclSetup;
import TextureGraphics.Memory.Array.ArrayData;
import TextureGraphics.Memory.Array.ArrayDataHandler;
import TextureGraphics.Memory.Array.IntArrayData;
import org.jocl.*;

import java.util.HashMap;

import static org.jocl.CL.*;
import static org.jocl.CL.CL_TRUE;

public class MultiTextureData implements ITextureData {

    cl_context context;
    cl_command_queue commandQueue;

    ArrayDataHandler textureArray, infoArray;

    public MultiTextureData(JoclSetup setup)
    {
        context = setup.getContext();
        commandQueue = setup.getCommandQueue();

        textureArray = new ArrayDataHandler(setup);
        infoArray = new ArrayDataHandler(setup);
    }

    public void init(int dataSize, int dataElements)
    {
        textureArray.init(dataSize, Sizeof.cl_int, CL_MEM_READ_ONLY);

        infoArray.init(3 * dataElements, Sizeof.cl_int, CL_MEM_READ_ONLY);
    }

    @Override
    public void create(int width, int height, int[] data, ITexture texture) {

        int startingOffset = textureArray.getMemoryOffset();

        textureArray.create(new IntArrayData(data), texture);

        int[] infoData = new int[]{ width, height, startingOffset / Sizeof.cl_int};
        infoArray.create(new IntArrayData(infoData), texture);
    }

    @Override
    public void update(int pos, int data, ITexture texture) {
        IntArrayData _package = (IntArrayData) textureArray.retrieveLocalData(texture, true);

        _package.data[pos] = data;
    }

    @Override
    public int getDataAt(int pos, ITexture texture) {
        IntArrayData _package = (IntArrayData) textureArray.retrieveLocalData(texture, false);

        return _package.data[pos];
    }

    @Override
    public int[] getTextureData(ITexture texture) {
        IntArrayData _package = (IntArrayData) textureArray.retrieveLocalData(texture, false);

        return _package.data;
    }

    @Override
    public int[] getTextureInfoData(ITexture texture) {
        IntArrayData _package = (IntArrayData) infoArray.retrieveLocalData(texture, false);

        return _package.data;
    }

    @Override
    public MemoryDataPackage getClTextureData()
    {
        cl_mem data = textureArray.retrieveDeviceData();

        //TODO why are these different? getClTextureData & getClTextureInfoData
        return new MemoryDataPackage(data, null);
    }

    @Override
    public Pointer getClTextureInfoData() {

        cl_mem data = infoArray.retrieveDeviceData();

        return Pointer.to(data);
    }
}
