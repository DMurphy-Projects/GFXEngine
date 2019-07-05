package TextureGraphics;

import TextureGraphics.Memory.Texture.ITexture;
import TextureGraphics.Memory.Texture.JoclTexture;

import java.awt.image.BufferedImage;

public abstract class JoclRenderer extends JoclProgram {

    //should either schedule or draw the polygon given
    public abstract void render(double[][] polygon, double[][] textureAnchor, ITexture texture);
    //should use the information generated from rendering the polygons to create a single screen
    //NOTE: this step may be unnecessary depending upon implementation
    public abstract BufferedImage createImage();
}
