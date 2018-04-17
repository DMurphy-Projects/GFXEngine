package GxEngine3D.Ordering;

import GxEngine3D.Model.Polygon3D;
import Shapes.IShape;

import java.util.ArrayList;

/**
 * Created by Dean on 31/12/16.
 */
public interface IOrderStrategy {
    public int[] order(double[] from, ArrayList<Polygon3D> polygons);
}
