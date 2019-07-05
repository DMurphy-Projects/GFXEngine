package TextureGraphics.Memory.Texture;

import org.jocl.Pointer;

public interface ITextureData {
    void create(int width, int height, int[] data);
    void update(int pos, int data, ITexture texture);

    int getDataAt(int pos, ITexture texture);

    int[] getTextureData();
    int[] getTextureInfoData();

    MemoryDataPackage getClTextureData();
    Pointer getClTextureInfoData();
}
