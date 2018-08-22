package GxEngine3D.Ordering;

import GxEngine3D.Model.Polygon3D;

import java.util.List;

/**
 * Created by Dean on 31/12/16.
 */
public interface IOrderStrategy {
    List<Integer> order(double[] from, List<Polygon3D> polygons);
}
