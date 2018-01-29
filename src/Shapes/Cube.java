package Shapes;

import java.awt.Color;

import GxEngine3D.RefPoint3D;
import GxEngine3D.ViewHandler;

public class Cube extends BaseShape {

	protected static String name = "Cube";
	
	public Cube(double x, double y, double z, double width, double length,
			double height, Color c, ViewHandler v) {

		super(x, y, z, width, length, height, c, v);
	}

	protected void createShape(Color[] c) {
		points.add(new RefPoint3D(x, y, z));// front bottom left
		points.add(new RefPoint3D(x + width, y, z));// front bottom right
		points.add(new RefPoint3D(x, y + length, z));// front top left
		points.add(new RefPoint3D(x + width, y + length, z));// front top right
		points.add(new RefPoint3D(x, y, z + height));// back bottom left
		points.add(new RefPoint3D(x + width, y, z + height));// back bottom
																// right
		points.add(new RefPoint3D(x, y + length, z + height));// back top left
		points.add(new RefPoint3D(x + width, y + length, z + height));// back
																		// top
																		// right

		add(new RefPoint3D[] { points.get(0), points.get(1), points.get(3),
				points.get(2) }, c[0]);// bottom
		add(new RefPoint3D[] { points.get(4), points.get(5), points.get(7),
				points.get(6) }, c[1]);// top
		add(new RefPoint3D[] { points.get(1), points.get(5), points.get(7),
				points.get(3) }, c[2]);// right
		add(new RefPoint3D[] { points.get(0), points.get(4), points.get(6),
				points.get(2) }, c[3]);// left
		add(new RefPoint3D[] { points.get(0), points.get(1), points.get(5),
				points.get(4) }, c[4]);// back
		add(new RefPoint3D[] { points.get(2), points.get(3), points.get(7),
				points.get(6) }, c[5]);// front
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
