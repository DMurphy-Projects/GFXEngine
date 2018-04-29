package ObjectFactory;

import java.util.ArrayList;

import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;

public class ShapeFactory {
	
	ArrayList<IProduct> shapes = new ArrayList<IProduct>();
	
	public BaseShape createObject(int i, double x, double y, double z)
	{
		return shapes.get(i).create(x, y, z);
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
