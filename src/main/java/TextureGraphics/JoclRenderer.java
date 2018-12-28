package TextureGraphics;

import TextureGraphics.Memory.JoclTexture;

import java.awt.image.BufferedImage;

public abstract class JoclRenderer extends JoclProgram {

    public JoclTexture createTexture(String path)
    {
        return new JoclTexture(path, context, commandQueue);
    }

    //should either schedule or draw the polygon given
    public abstract void render(double[][] polygon, double[][] textureAnchor, JoclTexture texture);
    //should use the information generated from rendering the polygons to create a single screen
    //NOTE: this step may be unnecessary depending upon implementation
    public abstract BufferedImage createImage();
}
