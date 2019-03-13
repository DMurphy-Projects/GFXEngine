package GxEngine3D.Helper;

import DebugTools.PaintPad;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

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

    private static double[] intersectAtZ0(double[] p0, double[] p1)
    {
        //intersect is starting point + some proportion of the vector of the line
        //Intersect (x, y, z) = (p0_x, p0_y, p0_z) + t2(p1_x - p0_x, p1_y - p0_y, p1_z - p0_z)
        double[] v = VectorCalc.sub(p1, p0);

        //take one element of the point
        //intersect z = p0_z + t(p1_z-p0_z)
        //z intersects at 0
        //p0_z + t(p1_z-p0_z) = 0
        //t = -p0_z / (p1_z-p0_z)
        double t = -p0[2] / v[2];

        return new double[]{
                p0[0] + (t * v[0]),
                p0[1] + (t * v[1]),
                0,//p0[2] + (t * (p1[2]-p0[2])),//technically this is always 0
        };
    }

    public static ArrayList<double[]> addPointsAtIntersect(double[][] polygon)
    {
        ArrayList<double[]> newPoly = new ArrayList<>();

        //iterate over edges from p0->p1, ...pn->p0
        for (int i=0;i<polygon.length;i++)
        {
            double[] p0 = polygon[i];
            double[] p1 = polygon[(i+1) % polygon.length];

            newPoly.add(p0);
            //they both need to be on different sides of the z-axis(+, -)
            if (Math.signum(p0[2]) != Math.signum(p1[2]))
            {
                double[] intersect = intersectAtZ0(p0, p1);
                newPoly.add(intersect);
            }
        }

        return newPoly;
    }

    public static double[][] recreateNearPlaneClipPolygon(double[][] polygon)
    {
        //debug
        pad.init();
        Polygon poly = pad.createPolygon(polygon, 30, 30);
        pad.drawPolygon(poly, Color.BLACK, false);
        //debug end

        ArrayList<double[]> newPoly = addPointsAtIntersect(polygon);

        //remove any points that are behind the near plane
        Iterator it = newPoly.iterator();
        while(it.hasNext())
        {
            double[] point = (double[]) it.next();

            if (point[2] < 0) {
                it.remove();
            }
        }

        //debug
        polygon = new double[newPoly.size()][];
        newPoly.toArray(polygon);

        poly = pad.createPolygon(polygon, 30, 30);
        pad.drawPolygon(poly, Color.RED, false);

        pad.finish();
        //debug end

        return polygon;
    }
}
