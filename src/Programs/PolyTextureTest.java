package Programs;

import DebugTools.DebugView;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Helper.FrameHelper;
import GxEngine3D.Helper.MatrixHelper;
import GxEngine3D.Helper.VectorCalc;
import GxEngine3D.Model.Matrix.Matrix;
import GxEngine3D.View.ViewHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PolyTextureTest {

    int[] texture;
    int tWidth, tHeight;

    int screenWidth, screenHeight;

    public static void main(String[] args) {

        PolyTextureTest test = new PolyTextureTest();
        test.init();
    }

    private void init()
    {
        String path = "/Resources/Textures/default.png";
        try {
            BufferedImage texture = ImageIO.read(new File(getClass().getResource(path).getFile()));
            this.tWidth = texture.getWidth();
            this.tHeight = texture.getHeight();
            this.texture = texture.getRGB(0, 0, texture.getWidth(), texture.getHeight(), null, 0, texture.getWidth());

        } catch (IOException e) {
        }

        screenWidth = 500; screenHeight = 500;
        int[] buffer = new int[screenWidth * screenHeight];
        DebugView view = new DebugView(screenWidth, screenHeight);
        FrameHelper.setupFrame(view, "Textures Test");

        double[][] relativePoints = new double[][]{
                new double[]{0, 0, 0},
                new double[]{1, 0, 0},
                new double[]{1, 1, 0},
                new double[]{0, 1, 0},
                new double[]{0.5, 0.5, 0},
        };

        double[][] tran = MatrixHelper.setupTranslateMatrix(0, 0, 0);
        double[][] rotate = MatrixHelper.setupFullRotation(0, 0, 0);
        double[][] scale = new double[4][4];
        scale[0][0] = 1;
        scale[1][1] = 1;
        scale[2][2] = 1;
        scale[3][3] = 1;

        Matrix combined = new Matrix(tran);
        combined = new Matrix(combined.matrixMultiply(rotate));
        combined = new Matrix(combined.matrixMultiply(scale));

        double[][] clipPoints = new double[relativePoints.length][];

        for (int i=0;i<relativePoints.length;i++)
        {
            clipPoints[i] = applyExplicitMatrix(combined, relativePoints[i]);
        }

        Camera c = new Camera(0, 0, -2);
        c.lookAt(new double[] {0, 0, 0});
        c.setup();

        ViewHandler vH = new ViewHandler(view, c, null);
        vH.updateMatrix();

        Matrix projectionMatrix = new Matrix(vH.getProjectionMatrix().matrixMultiply(c.getMatrix()));

        for (int i=0;i<clipPoints.length;i++)
        {
            clipPoints[i] = applyImplicitMatrix(projectionMatrix, clipPoints[i]);
        }

        double[][] relativeAnchor = new double[][]{
                clipPoints[0],
                clipPoints[1],
                clipPoints[2],
                clipPoints[3],
        };

        //anchoring on the centre gives much less of a warp
        buffer = drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[0],
                clipPoints[1],
        }, buffer, new double[][]{relativePoints[4], relativePoints[0], relativePoints[1]}, view,  relativeAnchor);

        buffer = drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[1],
                clipPoints[2],
        }, buffer, new double[][]{relativePoints[4], relativePoints[1], relativePoints[2]}, view,  relativeAnchor);

        buffer = drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[2],
                clipPoints[3],
        }, buffer, new double[][]{relativePoints[4], relativePoints[2], relativePoints[3]}, view,  relativeAnchor);

        buffer = drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[3],
                clipPoints[0],
        }, buffer, new double[][]{relativePoints[4], relativePoints[3], relativePoints[0]}, view,  relativeAnchor);

        view.update(buffer);
        view.repaint();

//        Polygon p = new Polygon();
//        for (double[] d:clipPoints)
//        {
//            int[] point = rasterProject(d, screenWidth, screenHeight);
//            p.addPoint(point[0], point[1]);
//        }

//        BufferedImage bi = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
////        bi.setRGB(0, 0, screenWidth, screenHeight, buffer, 0, screenWidth);
//        Graphics gx = bi.getGraphics();
//        gx.setColor(Color.black);
//        gx.fillRect(0, 0, screenWidth, screenHeight);
//        gx.setColor(Color.white);
//        gx.drawPolygon(p);
//        gx.dispose();
//
//        DebugView view1 = new DebugView(screenWidth, screenHeight);
//        FrameHelper.setupFrame(view1, "Graphics Version");
//        view1.update(bi.getRGB(0, 0, screenWidth, screenHeight, null, 0, screenWidth));
//        view1.repaint();
    }

    private double[] applyImplicitMatrix(Matrix m, double[] d)
    {
        d = m.pointMultiply(d);
        return new double[]{
                d[0] /= d[3],
                d[1] /= d[3],
                d[2] /= d[3]
        };
    }

    private double[] applyExplicitMatrix(Matrix m, double[] d)
    {
        d = m.pointMultiply(d);
        double absP = Math.abs(d[3]);
        return new double[]{
                d[0] / absP,
                d[1] / absP,
                d[2] / absP,
        };
    }

    //TODO reimplement function only moving horizontally in screen space to not skip points
    private int[] drawTriangle(double[][] triangle, int[] buffer, double[][] textureAnchor, DebugView view, double[][] relativeAnchor)
    {
        int[] r0 = rasterProject(triangle[0], screenWidth, screenHeight);
        int[] r1 = rasterProject(triangle[1], screenWidth, screenHeight);
        int[] r2 = rasterProject(triangle[2], screenWidth, screenHeight);

        //vectors of the length and height of the triangle
        double[] v01 = VectorCalc.sub(triangle[0], triangle[1]);
        double[] v02 = VectorCalc.sub(triangle[0], triangle[2]);

        //vectors of the length and height of the texture part we're going to use
        double[] t01 = VectorCalc.sub(textureAnchor[0], textureAnchor[1]);
        double[] t02 = VectorCalc.sub(textureAnchor[0], textureAnchor[2]);

        //length of the vectors in screen space, will be referred to as width/height
        double w =  Math.hypot(r1[0]-r0[0], r1[1]-r0[1]);
        double h =  Math.hypot(r2[0]-r0[0],  r2[1]-r0[1]);

        //texture height scaling, with perspective height can scale with width position
        double[] heightStartVector = VectorCalc.mul(VectorCalc.sub(relativeAnchor[3], relativeAnchor[0]), t02);
        double heightStart = Math.hypot(heightStartVector[0], heightStartVector[1]);
        double[] heightEndVector = VectorCalc.mul(VectorCalc.sub(relativeAnchor[2], relativeAnchor[1]), t02);
        double heightEnd = Math.hypot(heightEndVector[0], heightEndVector[1]);

        for (double i = 0; i < w; i ++) {
            double _i = i / w;
            //current height of the texture
            double currentHeight = heightStart / ((_i * (heightEnd-heightStart)) + heightStart);

            double[] p0 = VectorCalc.sub(triangle[0], VectorCalc.mul_v_d(v01, _i));

            double[] t0 = VectorCalc.sub(textureAnchor[0], VectorCalc.mul_v_d(t01, _i));

            //when _i is large, _ii must be small and vice-versa
            for (double ii = 0; ii <  (1-_i) * h; ii ++) {
                double _ii = ii / h;

                double[] p1 = VectorCalc.sub(p0, VectorCalc.mul_v_d(v02, _ii));
                int[] raster = rasterProject(p1, screenWidth, screenHeight);

                double[] t1 = VectorCalc.sub(t0, VectorCalc.mul_v_d(VectorCalc.mul_v_d(t02, currentHeight), _ii));
                //at this point t1 is the relative point(0-1) which gives a point on the texture
                int[] texturePoint = new int[] {
                        (int) (t1[0] * tWidth),
                        (int) (t1[1] * tHeight)
                };

                int color = getTextureAt(texturePoint[0], texturePoint[1]);
                buffer[view.getIndex(raster)] = color;
            }
        }
        return buffer;
    }

    private int getTextureAt(int w, int h)
    {
        if (w < 0 || h < 0 || w >= tWidth || h >= tHeight )
        {
            return (255 << 16) | (255 << 8 ) | 255;
        }
        int index = (h * tWidth) + w;
        return texture[index];
    }

    private int[] rasterProject(double[] p1, double width, double height)
    {
        double x = (p1[0] + 1) * 0.5 * width;
        double y = ((p1[1] + 1) * 0.5) * height;
        return new int[]{(int) (x), (int) (y)};
    }
}
