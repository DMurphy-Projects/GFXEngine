package GxEngine3D.Helper;

import DebugTools.PaintPad;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

//samples points of the edges in a polygon
public class PolygonPointSampler {

    private static PaintPad pad;

    public static void initDebug()
    {
        pad = new PaintPad(500, 500);
        pad.setMode(PaintPad.Mode.Centre);
    }

    //value is how many points we're adding
    public static double[][] samplePolygon(double[][] polygon, int value)
    {
        double[][] newPoly = new double[polygon.length + (polygon.length * value)][];

        for (int i=0;i<polygon.length;i++)
        {
            newPoly[i * (value+1)] = polygon[i];
            for (int k = 1; k <= value;k++)
            {
                newPoly[i * (value+1) + k] = sampleEdge(polygon[i], polygon[(i+1)%polygon.length], (double)(k) / (value+1));
            }
        }
        return newPoly;
    }

    public static double[] sampleEdge(double[] p0, double[] p1, double v)
    {
        double u = 1 - v;

        double[] point = new double[p0.length];
        for (int i=0;i<p0.length;i++)
        {
            point[i] = (p0[i] * u) + (p1[i] * v);
        }
        return point;
    }

    private static double[] avg(double[][] polygon)
    {
        double[] avg = new double[polygon[0].length];
        for (double[] d:polygon)
        {
            for (int i=0;i<polygon[0].length;i++) {
                avg[i] += d[i];
            }
        }
        for (int i=0;i<polygon[0].length;i++) {
            avg[i] /= polygon.length;
        }
        return avg;
    }

    public static double[][] recreateNearPlaneClipPolygon(double[][] polygon)
    {
        int sampleValue = 100;

        //TODO there is a problem with the clip polygon being extremely large when intersecting the near plane
        //as a result sampling at predetermined intervals might mean that some edges don't have any sampled points between the two points
        polygon = samplePolygon(polygon, sampleValue);
//        double[][] insidePoly = new double[polygon.length][];
//        for (int i=0;i<polygon.length;i++)
//        {
//            if (polygon[i][2] >= 0) {
//                insidePoly[i] = polygon[i];
//            }
//            else
//            {
//                insidePoly[i] = new double[]{0, 0, 0};
//            }
//        }

        ArrayList<double[]> newPolygon = new ArrayList<>();

        boolean findFront = true;
        int lastAdded = -1;
        for (int i=0;i<polygon.length;i++)
        {
            if (findFront) {
                //this is in front the nearPlane
                if (polygon[i][2] >= 0) {
                    newPolygon.add(polygon[i]);
                    System.out.println("Added: "+i+", "+polygon[i][2]);
                    findFront = false;
                    continue;
                }
            } else {
                if (lastAdded == i-1) { continue; }
                //this is behind the nearPlane
                if (polygon[i][2] <= 0) {
                        newPolygon.add(polygon[i - 1]);

                        System.out.println("Added_B: " + (i - 1) + ", " + polygon[i - 1][2]);
                        findFront = true;
                }

                if (i % (sampleValue+1) == 0)
                {
                    if (polygon[i][2] >= 0) {
                        newPolygon.add(polygon[i]);

                        System.out.println("Added_I: "+i+", "+polygon[i][2]);
                        lastAdded = i;
                    }
                }
            }
        }

        double[][] p = new double[newPolygon.size()][];

        newPolygon.toArray(p);

        //debug
        pad.init();
        Polygon poly;

        poly = pad.createPolygon(polygon, 30, 30);
        pad.drawPolygon(poly, Color.RED, false);

        poly = pad.createPolygon(p, 30, 30);
        pad.drawPolygon(poly, Color.BLACK, true);
        pad.drawPolygonVertices(poly, Color.RED, true, false);

//        poly = pad.createPolygon(insidePoly, 30, 30);
//        pad.drawPolygonVertices(poly, Color.BLUE, false);

        pad.finish();
        //debug end

        return p;
    }
}
