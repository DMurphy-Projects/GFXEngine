package ObjectFactory;

import java.awt.Color;
import java.util.Arrays;

import DebugTools.TextOutput;
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
		Cube c = new Cube(Color.BLUE);
		c.translate(x, y, z);
		return c;
	}

}
