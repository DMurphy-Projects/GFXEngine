package GxEngine3D.Ordering;

import GxEngine3D.Polygon3D;
import Shapes.IShape;

import java.util.ArrayList;

/**
 * Created by Dean on 31/12/16.
 */
//orders based on distance to each polygon
public class OrderPolygon extends BaseOrdering{
    @Override
    public int[] order(double[] from, ArrayList<IShape> shapes, ArrayList<Polygon3D> polygons) {
        double[] k = new double[polygons.size()];
        for (int i = 0; i < k.length; i++) {
            k[i] = polygons.get(i).getDist(from);
        }
        // new order for this shapes polygons
        return sortIndex(k);
    }
}
