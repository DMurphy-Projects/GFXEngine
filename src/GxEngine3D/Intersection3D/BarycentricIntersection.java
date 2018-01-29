package GxEngine3D.Intersection3D;

import GxEngine3D.CalculationHelper.VectorCalc;
import GxEngine3D.Model.Polygon3D;
import GxEngine3D.Model.Vector;

import java.util.ArrayList;

/**
 * Created by Dean on 13/01/17.
 */
public class BarycentricIntersection implements IIntersection3DStrategy {
    private IntersectionFinder3D finder = new IntersectionFinder3D();
    @Override
    public double[] intersects(double[] from, double[] point, double[] isect, ArrayList<Polygon3D> polysToCheck) {
        isect = finder.intersects(from, point, null, polysToCheck);
        for (Polygon3D poly:polysToCheck) {
            Vector v0 = new Vector(VectorCalc.sub_v3v3(poly.getShape()[0].toArray(), poly.getShape()[2].toArray()));
            Vector v1 = new Vector(VectorCalc.sub_v3v3(poly.getShape()[0].toArray(), poly.getShape()[1].toArray()));
            Vector v2 = new Vector(VectorCalc.sub_v3v3(poly.getShape()[0].toArray(), isect));

            double dot00 = v0.dot(v0);
            double dot01 = v0.dot(v1);
            double dot02 = v0.dot(v2);
            double dot11 = v1.dot(v1);
            double dot12 = v1.dot(v2);

            double invDenom = 1 / ((dot00 * dot11) - (dot01 * dot01));
            double u = ((dot11 * dot02) - (dot01 * dot12)) * invDenom;
            double v = ((dot00 * dot12) - (dot01 * dot02)) * invDenom;
            if((u >= -0.1) && (v >= -0.1) && (u + v < 1.1)) {
                return isect;
            }
        }
        return null;
    }
}
