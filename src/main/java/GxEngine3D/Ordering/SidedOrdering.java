package GxEngine3D.Ordering;

import GxEngine3D.Helper.PlaneCalc;
import GxEngine3D.Model.Plane;
import GxEngine3D.Model.Polygon3D;

import java.util.ArrayList;
import java.util.List;

//NOTE: according to JProfiler, order takes 73% of the total work being done
public class SidedOrdering extends BaseOrdering {
    @Override
    public List<Integer> order(double[] from, List<Polygon3D> polygons) {
        //init starting order
        List<Integer> order = new ArrayList<>();
        for (int i=0;i<polygons.size();i++)
        {
            order.add(i);
        }
        order = treeSort(order, polygons, from);
        return order;
    }

    //splits the array into 3 parts, the positive, the negative and the ambiguous
    //then recursively sorts those parts
    //similar to binary spacial partitions but only partial
    private List<Integer> treeSort(List<Integer> array, List<Polygon3D> polygons, double[] from)
    {
        if (array.size() <= 1) return array;
        ArrayList<Integer> pos = new ArrayList<>();
        ArrayList<Integer> neg = new ArrayList<>();
        int pivot = array.get(array.size()/2);
        Plane plane = new Plane(polygons.get(pivot));
        double[] nv = plane.getNV(from).toArray();
        for (int i:array)
        {
            if (pivot == i) continue;
            PlaneCalc.Side side = PlaneCalc.whichSide(polygons.get(i).getShape(), nv, plane.getP());
            //encompasses planar, negative and both sided nature
            if (side.ordinal() > 0)
            {
                neg.add(i);
            }
            else if (side == PlaneCalc.Side.POSITIVE)
            {
                pos.add(i);
            }
        }
        ArrayList<Integer> order = new ArrayList<>();
        order.addAll(treeSort(neg, polygons, from));
        order.add(pivot);
        order.addAll(treeSort(pos, polygons, from));
        return order;
    }
}
