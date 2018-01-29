package Shapes;

import java.awt.Color;

import GxEngine3D.RefPoint3D;
import GxEngine3D.ViewHandler;

public class Prism extends BaseShape {

	protected static String name = "Prism";
	
	public Prism(double x, double y, double z, double width, double length,
			double height, Color c, ViewHandler v) {
		super(x, y, z, width, length, height, c, v);
	}

	@Override
	protected void createShape() {
		points.add(new RefPoint3D(x, y, z));
		points.add(new RefPoint3D(x + width, y, z));
		points.add(new RefPoint3D(x + (width / 2), y + height, z));

		points.add(new RefPoint3D(x, y, z + length));
		points.add(new RefPoint3D(x + width, y, z + length));
		points.add(new RefPoint3D(x + (width / 2), y + height, z + length));

		// triangle faces
		add(new RefPoint3D[] { points.get(0), points.get(1), points.get(2) });
		add(new RefPoint3D[] { points.get(3), points.get(4), points.get(5) });

		// square faces
		add(new RefPoint3D[] { points.get(1), points.get(4), points.get(5),
				points.get(2) });
		add(new RefPoint3D[] { points.get(0), points.get(3), points.get(5),
				points.get(2) });
		add(new RefPoint3D[] { points.get(0), points.get(1), points.get(4),
				points.get(3) });
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
