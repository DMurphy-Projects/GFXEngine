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
		points.add(new RefPoint3D(x, y, z));
		points.add(new RefPoint3D(x + width, y, z));
		points.add(new RefPoint3D(x, y + height, z));
		points.add(new RefPoint3D(x + width, y + height, z));

		points.add(new RefPoint3D(x + (width / 2), y + (height / 2), z + length));

		add(new RefPoint3D[] { points.get(0), points.get(1), points.get(4) });
		add(new RefPoint3D[] { points.get(1), points.get(3), points.get(4) });
		add(new RefPoint3D[] { points.get(3), points.get(2), points.get(4) });
		add(new RefPoint3D[] { points.get(2), points.get(0), points.get(4) });

		// base
		add(new RefPoint3D[] { points.get(0), points.get(1), points.get(3),
				points.get(2) });
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
