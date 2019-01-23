package TextureGraphics.Memory.Texture;

import org.jocl.Pointer;

public interface ITexture {

    Pointer getTexture();
    Pointer getSize();
}
