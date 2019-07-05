package TextureGraphics.Memory.Texture;

import org.jocl.Pointer;

public interface ITextureData {
    void create(int width, int height, int[] data, ITexture texture);
    void update(int pos, int data, ITexture texture);

    int getDataAt(int pos, ITexture texture);

    int[] getTextureData(ITexture texture);
    int[] getTextureInfoData(ITexture texture);

    MemoryDataPackage getClTextureData();
    Pointer getClTextureInfoData();
}
