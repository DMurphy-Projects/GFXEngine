package Shapes;

import java.awt.Color;

import GxEngine3D.Model.RefPoint3D;
import GxEngine3D.View.ViewHandler;

public class Pyramid extends BaseShape {
	
	protected static String name = "Pyramid";
	
	public Pyramid(double x, double y, double z, double width, double length,
			double height, Color c, ViewHandler v) {
		super(x, y, z, width, length, height, c, v);
	}

	@Override
	protected void createShape() {
		points.add(new RefPoint3D(x, y, z));//0 front left
		points.add(new RefPoint3D(x + width, y, z));//1 front right
		points.add(new RefPoint3D(x, y + height, z));//2 back left
		points.add(new RefPoint3D(x + width, y + height, z));//3 back right
		points.add(new RefPoint3D(x + (width / 2), y + (height / 2), z + length));//4 top

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
