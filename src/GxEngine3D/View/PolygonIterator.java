package GxEngine3D.View;

import DebugTools.TextOutput;
import GxEngine3D.Model.Polygon3D;
import java.util.ArrayList;
import java.util.List;

public class PolygonIterator {

    ArrayList<Polygon3D> polygons;
    private List<Integer> newOrder;
    private int orderPos = 0;

    public PolygonIterator(ArrayList<Polygon3D> polys, List<Integer> order)
    {
        polygons = polys;
        newOrder = order;
    }
    public boolean hasNext()
    {
        return orderPos < newOrder.size();
    }
    public Polygon3D next()
    {
        Polygon3D d = polygons.get(newOrder.get(orderPos));
        TextOutput.println(newOrder.get(orderPos), 2);
		orderPos++;
		return d;
    }
}
