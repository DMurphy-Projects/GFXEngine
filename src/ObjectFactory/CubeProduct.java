package ObjectFactory;

import java.awt.Color;

import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;
import Shapes.Cube;

public class CubeProduct implements IProduct{

	@Override
	public String Name() {
		return Cube.getName();
	}

	@Override
	public BaseShape create(double x, double y, double z) {
		return new Cube(x, y, z, 1, 1, 1, Color.BLUE);
	}

}
