package Shapes;

import java.awt.Color;

import GxEngine3D.Model.RefPoint3D;

public class Prism extends BaseShape {

	protected static String name = "Prism";
	
	public Prism(Color c) {
		super(c);
	}

	@Override
	protected void createShape() {
		// triangle faces
		addPoint(new double[]{0, 0, 0});//0 front left
		addPoint(new double[]{1, 0, 0});//1 front right
		addPoint(new double[]{0.5, 1, 0});//2 front top

		addPoint(new double[]{0, 0, 1});//3 back left
		addPoint(new double[]{1, 0, 1});//4 back right
		addPoint(new double[]{0.5, 1, 1});//5 back top

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
