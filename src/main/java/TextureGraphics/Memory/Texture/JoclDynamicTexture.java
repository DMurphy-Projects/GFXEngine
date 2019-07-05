package TextureGraphics.Memory.Texture;

import Games.IGameScreen;
import TextureGraphics.Memory.BufferHelper;
import TextureGraphics.Memory.JoclMemoryMethods;
import org.jocl.*;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import static org.jocl.CL.*;

public class JoclDynamicTexture extends JoclTexture implements IGameScreen{

    public JoclDynamicTexture(String path, ITextureData dataHandler) {
        super(path, dataHandler);
    }

    @Override
    public void setPixel(int x, int y, int color) {
        dataHandler.update((y*image.getWidth())+x, color, this);
    }

    @Override
    public int getPixel(int x, int y) {
        return dataHandler.getDataAt((y*image.getWidth())+x, this);
    }

    @Override
    public int[] getScreenSize() {
        return new int[]{ image.getWidth(), image.getHeight()};
    }
}
