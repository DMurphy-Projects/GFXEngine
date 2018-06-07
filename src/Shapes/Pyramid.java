package Shapes;

import java.awt.Color;

import GxEngine3D.Model.RefPoint3D;

public class Pyramid extends BaseShape {
	
	protected static String name = "Pyramid";
	
	public Pyramid(Color c) {
		super(c);
	}

	@Override
	protected void createShape() {
		addPoint(new double[]{0, 0, 0});//0 front left
		addPoint(new double[]{1, 0, 0});//1 front right
		addPoint(new double[]{0, 1, 0});//2 back left
		addPoint(new double[]{1, 1, 0});//3 back right
		addPoint(new double[]{0.5, 0.5, 1});//4 top

		addEdge(new RefPoint3D[]{points.get(0), points.get(1)});//front
		addEdge(new RefPoint3D[]{points.get(1), points.get(3)});//right
		addEdge(new RefPoint3D[]{points.get(2), points.get(3)});//back
		addEdge(new RefPoint3D[]{points.get(2), points.get(0)});//left

		addEdge(new RefPoint3D[]{points.get(0), points.get(4)});//front left
		addEdge(new RefPoint3D[]{points.get(1), points.get(4)});//front right
		addEdge(new RefPoint3D[]{points.get(2), points.get(4)});//back left
		addEdge(new RefPoint3D[]{points.get(3), points.get(4)});//back right

		//clockwise facing object
		addPoly(new RefPoint3D[]{points.get(0), points.get(4), points.get(1)}, this.c);//front
		addPoly(new RefPoint3D[]{points.get(3), points.get(4), points.get(2)}, this.c);//back
		addPoly(new RefPoint3D[]{points.get(2), points.get(4), points.get(0)}, this.c);//left
		addPoly(new RefPoint3D[]{points.get(1), points.get(4), points.get(3)}, this.c);//right
		addPoly(new RefPoint3D[]{points.get(0), points.get(1), points.get(3), points.get(2)}, this.c);//base
	}
	
	@Override
	public String toString() {
		return getName();
	}
	public static String getName()
	{
		return Pyramid.name;
	}
}
