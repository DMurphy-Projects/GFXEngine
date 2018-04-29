package GxEngine3D.View;

import DebugTools.TextOutput;
import GxEngine3D.Model.Polygon2D;
import java.util.List;

public class PolygonIterator {

    List<Polygon2D> polygons;
    private List<Integer> newOrder;
    private int orderPos = 0;

    public PolygonIterator(List<Polygon2D> polys, List<Integer> order)
    {
        polygons = polys;
        newOrder = order;
    }
    public boolean hasNext()
    {
        return orderPos < newOrder.size();
    }
    public Polygon2D next()
    {
        Polygon2D d = polygons.get(newOrder.get(orderPos));
        TextOutput.println(newOrder.get(orderPos), 2);
		orderPos++;
		return d;
    }
}
