package TextureGraphics.CPUImplementation;

import DebugTools.DebugView;

public abstract class BaseRenderer {
    protected int getTextureAt(int w, int h, int tWidth, int tHeight, int[] texture)
    {
        if (w < 0 || h < 0 || w >= tWidth || h >= tHeight )
        {
            return (255 << 16) | (255 << 8 ) | 255;
        }
        int index = (h * tWidth) + w;
        return texture[index];
    }

    protected int[] rasterProject(double[] p1, double width, double height)
    {
        double x = ( p1[0] + 1) * 0.5 * width;
        double y = (1 - (p1[1] + 1) * 0.5) * height;
        return new int[]{(int) (x), (int) (y)};
    }

    public abstract int[] render(int[] buffer,
                                 double[] origin, double[] otherEnd, double[] travel,
                                 DebugView view,
                                 double[] textureOrigin, double[] textureV1, double[] textureV2,
                                 boolean direction,
                                 int screenWidth, int screenHeight,
                                 int[] texture, int tWidth, int tHeight);
}
