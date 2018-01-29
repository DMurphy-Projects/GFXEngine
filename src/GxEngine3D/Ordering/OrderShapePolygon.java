package GxEngine3D.Ordering;

import GxEngine3D.Model.Polygon3D;
import Shapes.IShape;

import java.util.ArrayList;

/**
 * Created by Dean on 31/12/16.
 */
//orders shapes then orders polygons
public class OrderShapePolygon extends BaseOrdering {
    @Override
    public int[] order(double[] from, ArrayList<IShape> shapes, ArrayList<Polygon3D> polygons) {
        // set values for first sort - sorting every shape based on distance
        // from lens
        // farthest to closest
        double[] k = new double[shapes.size()];
        for (int i = 0; i < k.length; i++) {
            k[i] = shapes.get(i).getDistanceFrom(from);
        }
        // shape order
        int[] order = sortIndex(k);

        int[] polyOrder = new int[polygons.size()];
        int polyI = 0;
        //System.out.println("S");
        //for (int i = order.length-1; i>=0; i--)
        for (int i = 0; i<order.length; i++)
        {
            //System.out.println(order[i]);
            // set values for second sort - sorting polygons based on distance
            // from lens
            // compounded with previous sort to achieve closest shape last then
            // closest polygon last in that shape
            IShape s = shapes.get(order[i]);

            //find how many polygons are before this one
            int polyBefore=0;
            for (int iii=0;iii<order[i];iii++)
            {
                polyBefore+= shapes.get(iii).getShape().size();
            }

            k = new double[s.getShape().size()];
            for (int ii = 0; ii < k.length; ii++) {
                k[ii] = s.getShape().get(ii).getDist(from);
            }
            // new order for this shapes polygons
            int[] o = sortIndex(k);
            for (int ii = 0; ii < o.length; ii++) {
                // o only has this shape and polyOrder has all
                // translation required
                // all polygons are grouped as shapes inside array list
                //o order is only in shape, polyorder needs to be for all shapes
                polyOrder[polyI] = o[ii] + polyBefore;//need to find where this corresponds with allPolygon array
                //System.out.println(order[i]+" "+(o[ii] + polyTot));
                polyI++;
            }
        }
         return polyOrder;
    }
}
