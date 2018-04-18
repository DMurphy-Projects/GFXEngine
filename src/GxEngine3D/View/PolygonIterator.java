package GxEngine3D.View;

import GxEngine3D.Model.Polygon3D;

import java.util.ArrayList;

public class PolygonIterator {

    ArrayList<Polygon3D> polygons;
    private int[] newOrder = new int[0];
    private int orderPos = 0;

    public PolygonIterator(ArrayList<Polygon3D> polys, int[] order)
    {
        polygons = polys;
        newOrder = order;
    }
    public boolean hasNext()
    {
        return orderPos < newOrder.length;
    }
    public Polygon3D next()
    {
        Polygon3D d = polygons.get(newOrder[orderPos]);
		orderPos++;
		return d;
    }
}
