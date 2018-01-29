package ObjectFactory;

import java.awt.Color;

import GxEngine3D.ViewHandler;
import Shapes.BaseShape;
import Shapes.Cube;

public class CubeProduct implements IProduct{

	@Override
	public String Name() {
		return Cube.getName();
	}

	@Override
	public BaseShape create(double x, double y, double z, ViewHandler v) {
		return new Cube(x, y, z, 1, 1, 1, Color.BLUE, v);
	}

}
