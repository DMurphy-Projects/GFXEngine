package ObjectFactory;

import java.util.ArrayList;

import GxEngine3D.ViewHandler;
import Shapes.BaseShape;

public class ShapeFactory {
	
	ArrayList<IProduct> shapes = new ArrayList<IProduct>();
	
	public BaseShape createObject(int i, double x, double y, double z, ViewHandler v)
	{
		return shapes.get(i).create(x, y, z, v);
	}
	public ArrayList<IProduct> shapeList()
	{
		return shapes;
	}
	public void add(IProduct i)
	{
		shapes.add(i);
	}
}
