package Programs;

import DebugTools.DebugView;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Helper.FrameHelper;
import GxEngine3D.Helper.MatrixHelper;
import GxEngine3D.Helper.Vector2DCalc;
import GxEngine3D.Helper.VectorCalc;
import GxEngine3D.Model.Matrix.Matrix;
import GxEngine3D.View.ViewHandler;
import TextureGraphics.CPUImplementation.BaseRenderer;
import TextureGraphics.CPUImplementation.PseudoKernelRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PolyTextureTest {

    int[] texture;
    int tWidth, tHeight;

    int screenWidth, screenHeight;

    double roll = 0, rollStep = 0;
    double yaw = 0, yawStep = 0.1;
    double pitch = 0, pitchStep = 0;

    public static void main(String[] args) {

        PolyTextureTest test = new PolyTextureTest();
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

    private int[] drawTriangle(double[][] triangle, int[] buffer, double[][] textureAnchor, DebugView view, BaseRenderer renderer) {
        double[] v01 = VectorCalc.sub(triangle[0], triangle[1]);
        double heightV01 = Math.abs(v01[1] * screenHeight);

        double[] v02 = VectorCalc.sub(triangle[0], triangle[2]);
        double heightV02 = Math.abs(v02[1] * screenHeight);

        double[] v03 = VectorCalc.sub(triangle[2], triangle[1]);

        if (Math.signum(v01[1]) != Math.signum(v02[1])) {
            //this fragment handles when the other two points are both above and below the origin
            //instead of coming from the centre going out, we start at the ends moving in
            double[] t01 = VectorCalc.sub(textureAnchor[1], textureAnchor[0]);//reversed
            double[] t02 = VectorCalc.sub(textureAnchor[2], textureAnchor[0]);//reversed
            double[] t03 = VectorCalc.sub(textureAnchor[2], textureAnchor[1]);

            double[] v04 = VectorCalc.sub(triangle[2], triangle[0]);

            double side = Vector2DCalc.side(triangle[2], triangle[1], triangle[0]) * -v04[1];

            buffer = renderer.render(buffer, triangle[2], triangle[1], v04, view,
                    textureAnchor[2], t02, t03, side > 0, screenWidth, screenHeight, texture, tWidth, tHeight);

            v04 = VectorCalc.sub(triangle[1], triangle[0]);
            side = Vector2DCalc.side(triangle[1], triangle[2], triangle[0]) * -v04[1];
            buffer = renderer.render(buffer, triangle[1], triangle[2], v04, view,
                    textureAnchor[1], t01, VectorCalc.mul_v_d(t03, -1), side > 0, screenWidth, screenHeight, texture, tWidth, tHeight);

        }
        else if (heightV01 > heightV02) {
            //this fragment handles whens both points are entirely above/below the origin, but the first traveling vector is longer
            //we should travel along the shortest edge first then pivot and do the rest of the longer edge
            double[] t01 = VectorCalc.sub(textureAnchor[0], textureAnchor[1]);
            double[] t02 = VectorCalc.sub(textureAnchor[0], textureAnchor[2]);

            double side = Vector2DCalc.side(triangle[1], triangle[2], triangle[0]) * -v02[1];
            buffer = renderer.render(buffer, triangle[0], triangle[1], v02, view,
                    textureAnchor[0], t02, t01, side > 0, screenWidth, screenHeight, texture, tWidth, tHeight);
            //if the heights are the same then we've already went over this portion
            if (heightV01 != heightV02) {
                v03 = VectorCalc.mul_v_d(v03, -1);

                double[] t03 = VectorCalc.sub(textureAnchor[1], textureAnchor[2]);
                double[] t04 = VectorCalc.sub(textureAnchor[1], textureAnchor[0]);

                side = Vector2DCalc.side(triangle[2], triangle[1], triangle[0]) * -v03[1];
                buffer = renderer.render(buffer, triangle[1], triangle[0],  v03, view,
                        textureAnchor[1], t03, t04, side > 0, screenWidth, screenHeight, texture, tWidth, tHeight);
            }
        }
        else {
            //this fragment handles when both points are entirely above/below the origin
            double[] t01 = VectorCalc.sub(textureAnchor[0], textureAnchor[1]);
            double[] t02 = VectorCalc.sub(textureAnchor[0], textureAnchor[2]);
            double[] t03 = VectorCalc.sub(textureAnchor[2], textureAnchor[1]);

            double side = Vector2DCalc.side(triangle[2], triangle[1], triangle[0]) * -v01[1];
            buffer = renderer.render(buffer, triangle[0], triangle[2], v01, view,
                    textureAnchor[0], t01, t02, side > 0, screenWidth, screenHeight, texture, tWidth, tHeight);
            //if the heights are the same then we've already went over this portion
            if (heightV01 != heightV02) {
                side = Vector2DCalc.side(triangle[1], triangle[2], triangle[0]) * -v03[1];
                buffer = renderer.render(buffer, triangle[2], triangle[0], v03, view,
                        textureAnchor[2], t03, VectorCalc.mul_v_d(t02, -1), side > 0, screenWidth, screenHeight, texture, tWidth, tHeight);
            }
        }
        return buffer;
    }
}
