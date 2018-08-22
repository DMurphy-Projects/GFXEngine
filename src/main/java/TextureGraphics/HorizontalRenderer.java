package TextureGraphics;

import DebugTools.DebugView;
import GxEngine3D.Helper.VectorCalc;

public class HorizontalRenderer extends BaseRenderer{

    //NOTE:
    //origin - origin of where we are drawing from
    //otherEnd - refers to the point that is not origin and is not a part of the travel vector
    //travel - vector of the direction in which we are drawing first, cannot be horizontal
    public int[] render(int[] buffer,
                             double[] origin, double[] otherEnd, double[] travel,
                             DebugView view,
                             double[] textureOrigin, double[] textureV1, double[] textureV2,
                             boolean direction,
                             int screenWidth, int screenHeight,
                             int[] texture, int tWidth, int tHeight)
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

                double[] p1 = VectorCalc.add(p0, new double[]{direction?_ii : -_ii, 0, 0});
                int[] raster = rasterProject(p1, screenWidth, screenHeight);
                if (view.isOutOfBounds(raster[0], raster[1])) continue;

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
        return buffer;
    }
}
