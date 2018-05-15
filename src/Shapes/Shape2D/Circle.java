package Shapes.Shape2D;

import java.awt.Color;
import java.util.ArrayList;

import GxEngine3D.Model.RefPoint3D;
import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;

public class Circle extends BaseShape {
	
	protected static String name = "Circle";

	protected int orientation = 1;

	public Circle(double x, double y, double z, double rad, Color c) {
		super(x, y, z, rad, rad, rad, c);
	}

	@Override
	protected void createShape() {
		RefPoint3D p, prevP = null;
		for (double i=0;i<Math.PI*2;i+=Math.PI/180)
		{
			if(orientation==0) {
				p = new RefPoint3D(x + (width * Math.cos(i)), y + (width * Math.sin(i)), z);
			}
			else if(orientation == 1) {
				p = new RefPoint3D(x, y + (width * Math.cos(i)), z + (width * Math.sin(i)));
			}
			else {
				p = new RefPoint3D(x + (width * Math.cos(i)), y, z + (width * Math.sin(i)));
			}
			points.add(p);
			if (prevP == null)
			{
				addEdge(new RefPoint3D[]{p, prevP});
			}
			prevP = p;
		}
		addEdge(new RefPoint3D[]{points.get(points.size()-1), points.get(0)});
		RefPoint3D[] s = new RefPoint3D[points.size()];
		points.toArray(s);
		addPoly(s, c);
	}
	public static String getName()
	{
		return Circle.name;
	}
	@Override
	public String toString() {
		return getName();
	}
}

