package GxEngine3D.Intersection3D;

import GxEngine3D.Polygon3D;
import GxEngine3D.RefPoint3D;

import java.util.ArrayList;

/**
 * Created by Dean on 13/01/17.
 */
public interface IIntersection3DStrategy {
    double[] intersects(double[] from, double[] point, double[] isect, ArrayList<Polygon3D> polysToCheck);
}
