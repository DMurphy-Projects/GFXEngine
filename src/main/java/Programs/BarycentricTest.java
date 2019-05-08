package Programs;

import DebugTools.DebugView;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Helper.*;
import GxEngine3D.Helper.Maths.MatrixHelper;
import GxEngine3D.Model.Matrix.Matrix;
import GxEngine3D.View.ViewHandler;
import TextureGraphics.CPUImplementation.BaseRenderer;
import TextureGraphics.CPUImplementation.PseudoKernelRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class BarycentricTest {

    int[] texture;
    int tWidth, tHeight;

    int screenWidth, screenHeight;

    double roll = 0, rollStep = 0;
    double yaw = 0, yawStep = 0.1;
    double pitch = 0, pitchStep = 0;

    public static void main(String[] args) {

        BarycentricTest test = new BarycentricTest();
        try {
            test.init();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void init() throws InterruptedException {
        String path = "resources/Textures/default.png";
        try {
            BufferedImage texture = ImageIO.read(new File(getClass().getClassLoader().getResource(path).getFile()));
            this.tWidth = texture.getWidth();
            this.tHeight = texture.getHeight();
            this.texture = texture.getRGB(0, 0, texture.getWidth(), texture.getHeight(), null, 0, texture.getWidth());

        } catch (IOException e) {
        }


        screenWidth = 500; screenHeight = 500;
        DebugView view = new DebugView(screenWidth, screenHeight);
        FrameHelper.setupFrame(view, "Textures Test");

        double[][] relativePoints = new double[][]{
                new double[]{0, 0, 0},
                new double[]{1, 0, 0},
                new double[]{1, 1, 0},
                new double[]{0, 1, 0},
                new double[]{0.5, 0.5, 0},
        };

        //due to the way images are stored, ie the first point is the top-left while the last point is bottom-right, these are not the same as relative points
        //so a different mapping must be used
        //the textureRelativePoints will map relative points to texture relative points, ie the first textureRelative point will correspond to the first relative point
        double[][] textureRelativePoints = new double[][] {
                new double[]{0, 1, 0},
                new double[]{1, 1, 0},
                new double[]{1, 0, 0},
                new double[]{0, 0, 0},
                new double[]{0.5, 0.5, 0},
        };

        Camera c = new Camera(0, 0, 2);
        c.lookAt(new double[] {0, 0, 0});
        c.setup();

        ViewHandler vH = new ViewHandler(view, c, null);
        vH.updateMatrix();

        DebugView view1 = new DebugView(screenWidth, screenHeight);
//        FrameHelper.setupFrame(view1, "TextureGraphics Version");

        view.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                double[][] tran = MatrixHelper.setupTranslateMatrix(0, 0, 0);
                double[][] rotate = MatrixHelper.setupFullRotation(pitch, yaw, roll);
                double[][] scale = new double[4][4];
                scale[0][0] = 1;
                scale[1][1] = 1;
                scale[2][2] = 1;
                scale[3][3] = 1;

                int[] buffer = new int[screenWidth * screenHeight];
                update(relativePoints, textureRelativePoints, tran, rotate, scale, vH, c, view, view1, buffer);
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        view.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                int dir = e.getWheelRotation();
                double[][] tran = MatrixHelper.setupTranslateMatrix(0, 0, 0);
                double[][] rotate = MatrixHelper.setupFullRotation(pitch+=pitchStep*dir, yaw+=yawStep*dir, roll += rollStep*dir);
                double[][] scale = new double[4][4];
                scale[0][0] = 1;
                scale[1][1] = 1;
                scale[2][2] = 1;
                scale[3][3] = 1;

                int[] buffer = new int[screenWidth * screenHeight];
                update(relativePoints, textureRelativePoints, tran, rotate, scale, vH, c, view, view1, buffer);
            }
        });
    }

    int count = 0;
    long total = 0;

    private void update(double[][] relativePoints, double[][] textureRelativePoints, double[][] tran, double[][] rotate, double[][] scale, ViewHandler vH, Camera c,
                         DebugView view, DebugView debugView, int[] buffer)
    {
        Matrix combined = new Matrix(tran);
        combined = new Matrix(combined.matrixMultiply(rotate));
        combined = new Matrix(combined.matrixMultiply(scale));

        double[][] clipPoints = new double[relativePoints.length][];

        for (int i=0;i<relativePoints.length;i++)
        {
            clipPoints[i] = applyExplicitMatrix(combined, relativePoints[i]);
        }

        Matrix projectionMatrix = new Matrix(vH.getProjectionMatrix().matrixMultiply(c.getMatrix()));

        for (int i=0;i<clipPoints.length;i++)
        {
            clipPoints[i] = applyImplicitMatrix(projectionMatrix, clipPoints[i]);
        }

        BaseRenderer renderer = new PseudoKernelRenderer();

        long start = System.nanoTime();

        //green
        buffer = drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[0],
                clipPoints[1],
        }, buffer, new double[][] {
                textureRelativePoints[4],
                textureRelativePoints[0],
                textureRelativePoints[1]}, view, renderer);

//        blues
        buffer = drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[1],
                clipPoints[2],
        }, buffer, new double[][]{
                textureRelativePoints[4],
                textureRelativePoints[1],
                textureRelativePoints[2]}, view, renderer);

        //red
        buffer = drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[2],
                clipPoints[3],
        }, buffer, new double[][]{
                textureRelativePoints[4],
                textureRelativePoints[2],
                textureRelativePoints[3]}, view, renderer);

//        yellow
        buffer = drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[3],
                clipPoints[0],
        }, buffer, new double[][]{
                textureRelativePoints[4],
                textureRelativePoints[3],
                textureRelativePoints[0]}, view, renderer);

        long end = System.nanoTime();
        if (count < 10)
        {
            total += end - start;
            count++;
        }
        else
        {
            System.out.println("Took " + (total / count) +" ns");
            count = 0;
            total = 0;
        }

        //handy debug stuff
        Polygon p = new Polygon();
        for (double[] d:clipPoints)
        {
            int[] point = rasterProject(d, screenWidth, screenHeight);
            p.addPoint(point[0], point[1]);
        }
        BufferedImage bi = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
//        bi.setRGB(0, 0, screenWidth, screenHeight, buffer, 0, screenWidth);
        Graphics gx = bi.getGraphics();
        gx.setColor(Color.black);
        gx.fillRect(0, 0, screenWidth, screenHeight);
        gx.setColor(Color.white);
        gx.drawPolygon(p);
        gx.dispose();
        debugView.update(bi.getRGB(0, 0, screenWidth, screenHeight, null, 0, screenWidth));
        debugView.repaint();

        view.update(buffer);
        view.repaint();
    }

    protected int[] rasterProject(double[] p1, double width, double height)
    {
        double x = ( p1[0] + 1) * 0.5 * width;
        double y = (1 - (p1[1] + 1) * 0.5) * height;
        return new int[]{(int) (x), (int) (y)};
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

    protected int getTextureAt(int w, int h, int tWidth, int tHeight, int[] texture)
    {
        if (w < 0 || h < 0 || w >= tWidth || h >= tHeight )
        {
            return (255 << 16) | (255 << 8 ) | 255;
        }
        int index = (h * tWidth) + w;
        return texture[index];
    }

    //TODO better relationship between points to sample and how many point we are actually sampling
    //NOTE compared to other methods to this point, this method is more than an order of magnitude faster
    private int[] drawTriangle(double[][] t, int[] buffer, double[][] textureAnchor, DebugView view, BaseRenderer renderer) {

        int[] p1 = rasterProject(t[0], screenWidth, screenHeight);
        int[] p2 = rasterProject(t[1], screenWidth, screenHeight);
        int[] p3 = rasterProject(t[2], screenWidth, screenHeight);


        //calc area
        double inc = (-p2[0]*p3[1]) - (p1[0]*p2[1]) + (p1[0]*p3[1]) + (p2[0]*p1[1]) + (p3[0]*p2[1]) - (p3[0]*p1[1]);
        inc = Math.abs(inc/2);
        inc = Math.ceil(Math.sqrt(inc));


        //sampling based on area seems sparse
        inc *= 2;
        //create step
        inc = 1d / inc;

        //testing relative calculations
        //NOTE: calculation can happen in relative space and is approx eqiv. to twice that of the pixel space variant
//        double inc2 = (-t[1][0]*t[2][1]) - (t[0][0]*t[1][1]) + (t[0][0]*t[2][1]) + (t[1][0]*t[0][1]) + (t[2][0]*t[1][1]) - (t[2][0]*t[0][1]);
//        inc2 = Math.abs(screenHeight * screenWidth * inc2 * .5);
//        inc2 = Math.ceil(Math.sqrt(inc2));

        for (double u = 0;u<1;u+=inc)
        {
            for (double v = 0;v<1-u;v+= inc)
            {
                double w = 1 - u - v;
                //convert barycentric coords to cartesian relative coords for the relative triangle
                double[] p = new double[2];
                p[0] = (u * t[0][0]) + (v * t[1][0]) + (w * t[2][0]);
                p[1] = (u * t[0][1]) + (v * t[1][1]) + (w * t[2][1]);

                int[] raster = rasterProject(p, screenWidth, screenHeight);

                //convert barycentric coords to cartesian for relative texture triangle
                int[] texture = new int[2];
                texture[0] = (int)(tWidth * ((u * textureAnchor[0][0]) + (v * textureAnchor[1][0]) + (w * textureAnchor[2][0])));
                texture[1] = (int)(tHeight * ((u * textureAnchor[0][1]) + (v * textureAnchor[1][1]) + (w * textureAnchor[2][1])));

                int color = getTextureAt(texture[0], texture[1], tWidth, tHeight, this.texture);

                int pos = view.getIndex(raster);
//                buffer[pos] = (buffer[pos]+1) << 2;
                buffer[pos] = color;
            }
        }
        return buffer;
    }
}
