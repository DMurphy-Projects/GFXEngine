package TextureGraphics;

import DebugTools.DebugView;
import GxEngine3D.Helper.VectorCalc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PseudoKernelRenderer extends BaseRenderer {

    ExecutorService executor = Executors.newFixedThreadPool(10);

    @Override
    public int[] render(int[] buffer,
                               double[] origin, double[] otherEnd, double[] travel,
                               DebugView view,
                               double[] textureOrigin, double[] textureV1, double[] textureV2,
                               boolean direction,
                               int screenWidth, int screenHeight,
                               int[] texture, int tWidth, int tHeight)
    {


        double height = Math.abs(travel[1] * screenHeight);
        double[] other = VectorCalc.sub(origin, otherEnd);

        List<Callable<Void>> runnables = new ArrayList<>();
        for (int i=0;i<height;i++)
        {
            int finalI = i;
            runnables.add(() -> {
                PseudoKernelRenderer.this.kernel_outer_render(buffer,
                        finalI,
                        origin, otherEnd, travel,
                        view,
                        textureOrigin, textureV1, textureV2,
                        direction,
                        screenWidth, screenHeight,
                        texture, tWidth, tHeight,
                        height, other);
                return null;
            });
        }
        try {
            executor.invokeAll(runnables);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    private void kernel_outer_render(int[] buffer, int i,double[] origin, double[] otherEnd, double[] travel,
                                     DebugView view,
                                     double[] textureOrigin, double[] textureV1, double[] textureV2,
                                     boolean direction,
                                     int screenWidth, int screenHeight,
                                     int[] texture, int tWidth, int tHeight,
                                     double height, double[] other)  {
        //*outer for loop
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

        for (int ii=0;ii<width;ii++)
        {
            kernel_inner_render(buffer,
                    _i, ii,
                    origin, otherEnd, travel,
                    view,
                    textureOrigin ,textureV1, textureV2,
                    direction,
                    screenWidth, screenHeight,
                    texture, tWidth, tHeight,
                    width, p0);
        }
    }

    private void kernel_inner_render(int[] buffer, double _i, int ii,double[] origin, double[] otherEnd, double[] travel,
                                       DebugView view,
                                       double[] textureOrigin, double[] textureV1, double[] textureV2,
                                       boolean direction,
                                       int screenWidth, int screenHeight,
                                       int[] texture, int tWidth, int tHeight,
                                       double width, double[] p0)
    {
        //this is the horizontal relative co-ord, not the relative co-ord of the other vector that makes up the triangle
        double _ii = (double)ii / (screenWidth / 2);

        double[] p1 = VectorCalc.add(p0, new double[]{direction?_ii : -_ii, 0, 0});
        int[] raster = rasterProject(p1, screenWidth, screenHeight);
        if (view.isOutOfBounds(raster[0], raster[1])) return;

        double test = 1 - (ii / width);

        //this is the position on the other side
        double[] p2 = VectorCalc.sub(p1, VectorCalc.mul_v_d(VectorCalc.mul_v_d(travel, -_i), test));
        double[] pv01 = VectorCalc.sub(p2, origin);
        double newII = VectorCalc.len(pv01) / VectorCalc.len(VectorCalc.sub(otherEnd, origin));

        double[] pv02 = VectorCalc.sub(VectorCalc.sub(p1, pv01), origin);
        double newI = VectorCalc.len(pv02) / VectorCalc.len(travel);

        double[] t0 = VectorCalc.sub(textureOrigin, VectorCalc.mul_v_d(textureV1, newI));

        double[] t1 = VectorCalc.sub(t0, VectorCalc.mul_v_d(textureV2, newII));
        int[] texturePoint = new int[] {
                (int) (t1[0] * tWidth),
                (int) (t1[1] * tHeight)
        };
        int color = getTextureAt(texturePoint[0], texturePoint[1], tWidth, tHeight, texture);

        buffer[view.getIndex(raster)] = color;
    }
}
