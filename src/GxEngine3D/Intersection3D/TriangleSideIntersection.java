package GxEngine3D.Intersection3D;

import GxEngine3D.CalculationHelper.VectorCalc;
import GxEngine3D.Plane;
import GxEngine3D.Polygon3D;
import GxEngine3D.RefPoint3D;
import GxEngine3D.Vector;

import java.util.ArrayList;

/**
 * Created by Dean on 13/01/17.
 */
public class TriangleSideIntersection implements IIntersection3DStrategy {
    private IntersectionFinder3D finder = new IntersectionFinder3D();
    private boolean SameSide(double[] p1, double[] p2, double[] a, double[] b)
    {
        Vector cp1 = new Vector(VectorCalc.sub_v3v3(b, a)).crossProduct(new Vector(VectorCalc.sub_v3v3(p1, a)));
        Vector cp2 = new Vector(VectorCalc.sub_v3v3(b, a)).crossProduct(new Vector(VectorCalc.sub_v3v3(p2, a)));
        return (cp1.dot(cp2) >= 0);
    }
    private boolean PointInTriangle(double[] p, double[] a, double[] b, double[] c) {
        return (SameSide(p, a, b, c) && SameSide(p, b, a, c)
                && SameSide(p, c, a, b));
    }
    @Override
    public double[] intersects(double[] from, double[] point, double[] isect, ArrayList<Polygon3D> polysToCheck) {
        isect = finder.intersects(from, point, null, polysToCheck);
        Vector v = new Vector(VectorCalc.sub_v3v3(from, isect));
        if (v.Length() == 0)return null;//is intersection on the from side
        for (Polygon3D poly:polysToCheck)
        {
            RefPoint3D[] shape = poly.getShape();
            if (PointInTriangle(isect, shape[0].toArray(), shape[1].toArray(), shape[2].toArray()))
            {
                //System.out.println("Bounced");
                return isect;
            }
        }
        return null;
    }
}
