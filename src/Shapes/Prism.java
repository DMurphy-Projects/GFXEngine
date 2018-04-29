package Shapes;

import java.awt.Color;

import GxEngine3D.Model.RefPoint3D;
import GxEngine3D.View.ViewHandler;

public class Prism extends BaseShape {

	protected static String name = "Prism";
	
	public Prism(double x, double y, double z, double width, double length,
			double height, Color c) {
		super(x, y, z, width, length, height, c);
	}

	@Override
	protected void createShape() {
		// triangle faces
		points.add(new RefPoint3D(x, y, z));//0 front left
		points.add(new RefPoint3D(x + width, y, z));//1 front right
		points.add(new RefPoint3D(x + (width / 2), y + height, z));//2 front top

		points.add(new RefPoint3D(x, y, z + length));//3 back left
		points.add(new RefPoint3D(x + width, y, z + length));//4 back right
		points.add(new RefPoint3D(x + (width / 2), y + height, z + length));//5 back top

		addEdge(new RefPoint3D[]{points.get(0), points.get(1)});//front bottom
		addEdge(new RefPoint3D[]{points.get(0), points.get(2)});//front left
		addEdge(new RefPoint3D[]{points.get(2), points.get(1)});//front right
		addEdge(new RefPoint3D[]{points.get(3), points.get(4)});//back bottom
		addEdge(new RefPoint3D[]{points.get(3), points.get(5)});//back left
		addEdge(new RefPoint3D[]{points.get(5), points.get(4)});//back right

		addEdge(new RefPoint3D[]{points.get(0), points.get(3)});//left side
		addEdge(new RefPoint3D[]{points.get(1), points.get(4)});//right side
		addEdge(new RefPoint3D[]{points.get(2), points.get(5)});//top side

		//clockwise facing object
		addPoly(new RefPoint3D[]{points.get(0), points.get(2), points.get(1)}, c);//front face
		addPoly(new RefPoint3D[]{points.get(4), points.get(5), points.get(3)}, c);//back face
		addPoly(new RefPoint3D[]{points.get(0), points.get(1), points.get(4), points.get(3)}, c);//bottom
		addPoly(new RefPoint3D[]{points.get(3), points.get(5), points.get(2), points.get(0)}, c);//left face

		addPoly(new RefPoint3D[]{points.get(1), points.get(2), points.get(5), points.get(4)}, c);//right face
	}
	public static String getName()
	{
		return Prism.name;
	}
	@Override
	public String toString() {
		return getName();
	}
}
