package ObjectFactory;

import java.awt.Color;

import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;
import Shapes.Pyramid;

public class PyramidProduct implements IProduct{

	@Override
	public String Name() {
		return Pyramid.getName();
	}

	@Override
	public BaseShape create(double x, double y, double z) {
		return new Pyramid(x, y, z, 1, 1, 1, Color.BLUE);
	}

}
