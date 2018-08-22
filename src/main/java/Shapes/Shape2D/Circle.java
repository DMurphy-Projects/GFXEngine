package Shapes.Shape2D;

import java.awt.Color;

import GxEngine3D.Model.RefPoint3D;
import Shapes.BaseShape;

public class Circle extends BaseShape {
	
	protected static String name = "Circle";
	protected int orientation = 1;

	public Circle(Color c) {
		super(c);
	}

	@Override
	protected void createShape() {
		int index = 0;
		for (double i=0;i<Math.PI*2;i+=Math.PI/180)
		{
			double[] p;
			if(orientation==0) {
				p = new double[]{Math.cos(i), Math.sin(i), 0};
			}
			else if(orientation == 1) {
				p = new double[]{0, Math.cos(i), Math.sin(i)};
			}
			else {
				p = new double[]{Math.cos(i), 0, Math.sin(i)};
			}
			addPoint(p);
			if (index > 0)
			{
				addEdge(new RefPoint3D[]{points.get(index), points.get(index-1)});
			}
		}
		addEdge(new RefPoint3D[]{points.get(points.size()-1), points.get(0)});
		RefPoint3D[] s = new RefPoint3D[relativePoints.size()];
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

