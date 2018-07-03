package Programs;

import DebugTools.DebugView;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Helper.FrameHelper;
import GxEngine3D.Helper.MatrixHelper;
import GxEngine3D.Helper.VectorCalc;
import GxEngine3D.Model.Matrix.Matrix;
import GxEngine3D.View.ViewHandler;

import javax.imageio.ImageIO;
import java.awt.*;
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

        Camera c = new Camera(0, 0, 2);
        c.lookAt(new double[] {0, 0, 0});
        c.setup();

        ViewHandler vH = new ViewHandler(view, c, null);
        vH.updateMatrix();

        Matrix projectionMatrix = new Matrix(vH.getProjectionMatrix().matrixMultiply(c.getMatrix()));

        for (int i=0;i<clipPoints.length;i++)
        {
            clipPoints[i] = applyImplicitMatrix(projectionMatrix, clipPoints[i]);
        }

        //red
        buffer = drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[0],
                clipPoints[1],
        }, buffer, new double[][] {
                textureRelativePoints[4],
                textureRelativePoints[0],
                textureRelativePoints[1]}, view);

        //blue
        buffer = drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[1],
                clipPoints[2],
        }, buffer, new double[][]{
                textureRelativePoints[4],
                textureRelativePoints[1],
                textureRelativePoints[2]}, view);

        //green
        buffer = drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[2],
                clipPoints[3],
        }, buffer, new double[][]{
                textureRelativePoints[4],
                textureRelativePoints[2],
                textureRelativePoints[3]}, view);

        //yellow
        buffer = drawTriangle(new double[][] {
                clipPoints[4],
                clipPoints[3],
                clipPoints[0],
        }, buffer, new double[][]{
                textureRelativePoints[4],
                textureRelativePoints[3],
                textureRelativePoints[0]}, view);

        view.update(buffer);
        view.repaint();

        //to test where the relative points end up on the screen so they can be sync'd with the manual polygon drawing
//        Polygon p = new Polygon();
//        int[] point = rasterProject(clipPoints[4], screenWidth, screenHeight);
//        p.addPoint(point[0], point[1]);
//        point = rasterProject(clipPoints[0], screenWidth, screenHeight);
//        p.addPoint(point[0], point[1]);
//        point = rasterProject(clipPoints[1], screenWidth, screenHeight);
//        p.addPoint(point[0], point[1]);

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

    private int[] drawTriangle(double[][] triangle, int[] buffer, double[][] textureAnchor, DebugView view) {
        double[] v01 = VectorCalc.sub(triangle[0], triangle[1]);
        double heightV01 = Math.abs(v01[1] * screenHeight);

        double[] v02 = VectorCalc.sub(triangle[0], triangle[2]);
        double heightV02 = Math.abs(v02[1] * screenHeight);

        double[] v03 = VectorCalc.sub(triangle[2], triangle[1]);

        if (Math.signum(v01[1]) != Math.signum(v02[1])) {
            //special handling for the case where the usual origin of the triangle is in the middle of the other two points
            //instead of coming from the centre going out, we start at the ends moving in
            double[] t01 = VectorCalc.sub(textureAnchor[1], textureAnchor[0]);//reversed
            double[] t02 = VectorCalc.sub(textureAnchor[2], textureAnchor[0]);//reversed
            double[] t03 = VectorCalc.sub(textureAnchor[2], textureAnchor[1]);

            buffer = horizontal(buffer, triangle[2], triangle[1], VectorCalc.sub(triangle[2], triangle[0]), view,
                    textureAnchor[2], t02, t03);

            buffer = horizontal(buffer, triangle[1], triangle[2], VectorCalc.sub(triangle[1], triangle[0]), view,
                    textureAnchor[1], t01, VectorCalc.mul_v_d(t03, -1));

        } else if (heightV01 > heightV02) {
            //we should travel along the shortest edge first then pivot and do the rest of the longer edge
            double[] t01 = VectorCalc.sub(textureAnchor[0], textureAnchor[1]);
            double[] t02 = VectorCalc.sub(textureAnchor[0], textureAnchor[2]);

            buffer = horizontal(buffer, triangle[0], triangle[1], v02, view,
                    textureAnchor[0], t01, t02);
            //if the heights are the same then we've already went over this portion
            if (heightV01 != heightV02) {
                buffer = horizontal(buffer, triangle[1], triangle[0], VectorCalc.mul_v_d(v03, -1), view,
                        textureAnchor[0], t01, t02);
            }
        }
        else {
            double[] t01 = VectorCalc.sub(textureAnchor[0], textureAnchor[1]);
            double[] t02 = VectorCalc.sub(textureAnchor[0], textureAnchor[2]);
            double[] t03 = VectorCalc.sub(textureAnchor[2], textureAnchor[1]);

            buffer = horizontal(buffer, triangle[0], triangle[2], v01, view,
                    textureAnchor[0], t01, t02);
            //if the heights are the same then we've already went over this portion
            if (heightV01 != heightV02) {
                buffer = horizontal(buffer, triangle[2], triangle[0], v03, view,
                        textureAnchor[2], t03, VectorCalc.mul_v_d(t02, -1));
            }
        }
        return buffer;
    }

    //NOTE:
    //origin - origin of where we are drawing from
    //otherEnd - refers to the point that is not origin and is not a part of the travel vector
    //travel - vector of the direction in which we are drawing first, cannot be horizontal
    private int[] horizontal(int[] buffer, double[] origin, double[] otherEnd, double[] travel, DebugView view,
                             double[] textureOrigin, double[] textureV1, double[] textureV2)
    {
        double height = Math.abs(travel[1] * screenHeight);
        //if travel is the vector we will start horizontal lines at then other is the vector where horizontal lines end
        double[] other = VectorCalc.sub(origin, otherEnd);

        for (int i=0;i<height;i++)
        {
            double _i = i / height;
            double[] p0 = VectorCalc.sub(origin, VectorCalc.mul_v_d(travel, _i));

            //must find point on line where y=p0[1]
            //y = mx + c
            //v01 => (x2-x1, y2-y1)
            double m = other[1] / other[0];
            double x;
            if (Double.isInfinite(m))
            {
                x = origin[0];
            }
            else
            {
                //y1 = m(x1) + c => c = y1 - m(x1)
                double c = origin[1] - (m * origin[0]);
                //mx = y - c => x = (y - c) / m
                x = (p0[1] - c) / m;
            }

            double width = Math.abs(screenWidth * (x - p0[0]) / 2);

            for (double ii=0; ii < width; ii++) {
                //this is the horizontal relative co-ord, not the relative co-ord of the other vector that makes up the triangle
                double _ii = ii / (screenWidth / 2);

                double[] p1 = VectorCalc.add(p0, new double[]{travel[0] > 0?_ii : -_ii, 0, 0});

                double test = 1 - (ii / width);

                //this is the position on the other side
                double[] p2 = VectorCalc.sub(p1, VectorCalc.mul_v_d(VectorCalc.mul_v_d(travel, -_i), test));
                double[] pv01 = VectorCalc.sub(p2, origin);
                double newII = VectorCalc.len(pv01) / VectorCalc.len(VectorCalc.sub(otherEnd, origin));
//                buffer[view.getIndex(rasterProject(p2, screenWidth, screenHeight))] =  (255 << 16) | (255 << 8 ) | 255;

                double[] pv02 = VectorCalc.sub(VectorCalc.sub(p1, pv01), origin);
                double newI = VectorCalc.len(pv02) / VectorCalc.len(travel);
//                buffer[view.getIndex(rasterProject(p3, screenWidth, screenHeight))] =  (255 << 16) | (255 << 8 ) | 255;

                double[] t0 = VectorCalc.sub(textureOrigin, VectorCalc.mul_v_d(textureV1, newI));

                double[] t1 = VectorCalc.sub(t0, VectorCalc.mul_v_d(textureV2, newII));
                int[] texturePoint = new int[] {
                        (int) (t1[0] * tWidth),
                        (int) (t1[1] * tHeight)
                };
                int color = getTextureAt(texturePoint[0], texturePoint[1]);
//                int color = (255 << 16) | (255 << 8 ) | 255;

                int[] raster = rasterProject(p1, screenWidth, screenHeight);
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
        double x = ( p1[0] + 1) * 0.5 * width;
        double y = (1 - (p1[1] + 1) * 0.5) * height;
        return new int[]{(int) (x), (int) (y)};
    }
}
