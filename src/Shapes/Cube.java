package Shapes;

import java.awt.Color;

import GxEngine3D.Model.RefPoint3D;

public class Cube extends BaseShape {

	protected static String name = "Cube";
	
	public Cube(Color c) {
		super(c);
	}

	protected void createShape(Color[] c) {
		addPoint(new double[]{0, 0, 0});//0 bottom back left
		addPoint(new double[]{1, 0, 0});//1 bottom back right
		addPoint(new double[]{0, 1, 0});//2 bottom front left
		addPoint(new double[]{1, 1, 0});//3 bottom front right
		addPoint(new double[]{0, 0, 1});//4 top back left
		addPoint(new double[]{1, 0, 1});//5 top back right
		addPoint(new double[]{0, 1,  1});//6 top front left
		addPoint(new double[]{1, 1, 1});//7 top front right

		addEdge(new RefPoint3D[]{points.get(3), points.get(2)});//bottom front
		addEdge(new RefPoint3D[]{points.get(3), points.get(1)});//bottom right
		addEdge(new RefPoint3D[]{points.get(0), points.get(1)});//bottom back
		addEdge(new RefPoint3D[]{points.get(0), points.get(2)});//bottom left

		addEdge(new RefPoint3D[]{points.get(7), points.get(6)});//top front
		addEdge(new RefPoint3D[]{points.get(7), points.get(5)});//top right
		addEdge(new RefPoint3D[]{points.get(4), points.get(5)});//top back
		addEdge(new RefPoint3D[]{points.get(6), points.get(4)});//top left

		addEdge(new RefPoint3D[]{points.get(7), points.get(3)});//front right
		addEdge(new RefPoint3D[]{points.get(2), points.get(6)});//front left
		addEdge(new RefPoint3D[]{points.get(0), points.get(4)});//back left
		addEdge(new RefPoint3D[]{points.get(5), points.get(1)});//back right

		//clockwise facing object
		addPoly(new RefPoint3D[]{points.get(0), points.get(2), points.get(3), points.get(1)}, c[0]);//bottom
		addPoly(new RefPoint3D[]{points.get(4), points.get(5), points.get(7), points.get(6)}, c[1]);//top
		addPoly(new RefPoint3D[]{points.get(6), points.get(7), points.get(3), points.get(2)}, c[2]);//front
		addPoly(new RefPoint3D[]{points.get(0), points.get(1), points.get(5), points.get(4)}, c[3]);//back
		addPoly(new RefPoint3D[]{points.get(6), points.get(2), points.get(0), points.get(4)}, c[4]);//left
		addPoly(new RefPoint3D[]{points.get(3), points.get(7), points.get(5), points.get(1)}, c[5]);//right
	}

	@Override
	protected void createShape() {
		createShape(new Color[] { c, c, c, c, c, c });
	}
	public static String getName()
	{
		return Cube.name;
	}
	@Override
	public String toString() {
		return getName();
	}

}
