package GxEngine3D.Intersection3D;

import GxEngine3D.CalculationHelper.VectorCalc;
import GxEngine3D.Plane;
import GxEngine3D.Polygon3D;
import GxEngine3D.Projection;

import java.util.ArrayList;

/**
 * Created by Dean on 13/01/17.
 * note that this method does not actually find if the vector intersects the polys
 */
public class IntersectionFinder3D implements IIntersection3DStrategy{
    @Override
    public double[] intersects(double[] from, double[] p, double[] isect, ArrayList<Polygon3D> polysToCheck) {
        Plane plane = new Plane(polysToCheck.get(0));
        Projection pr = VectorCalc.isect_line_plane_perspective(from, new double[]{ p[0], p[1], p[2]},
                plane.getP(), plane.getFlipNV().toArray());
        return pr.Point();
    }
}
