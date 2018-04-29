package Shapes.Shape2D;

import java.awt.Color;
import java.util.ArrayList;

import GxEngine3D.Model.RefPoint3D;
import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;

public class Circle extends BaseShape {
	
	protected static String name = "Circle";

	protected int orientation = 0;

	public Circle(double x, double y, double z, double rad, Color c) {
		super(x, y, z, rad, rad, rad, c);
	}

	@Override
	protected void createShape() {

		ArrayList<RefPoint3D> shape = new ArrayList<RefPoint3D>();
		
		RefPoint3D p;
		for (double i=0;i<Math.PI*2;i+=Math.PI/180)
		{
			if(orientation==0)
				p = new RefPoint3D(x+(width*Math.cos(i)), y+(width*Math.sin(i)), z);
			else if(orientation == 1)
				p = new RefPoint3D(x, y+(width*Math.cos(i)), z+(width*Math.sin(i)));
			else
				p = new RefPoint3D(x+(width*Math.cos(i)), y, z+(width*Math.sin(i)));
			points.add(p);
			shape.add(p);
		}
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

