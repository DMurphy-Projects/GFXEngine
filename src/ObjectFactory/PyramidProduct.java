package ObjectFactory;

import java.awt.Color;

import GxEngine3D.ViewHandler;
import Shapes.BaseShape;
import Shapes.Pyramid;

public class PyramidProduct implements IProduct{

	@Override
	public String Name() {
		return Pyramid.getName();
	}

	@Override
	public BaseShape create(double x, double y, double z, ViewHandler v) {
		return new Pyramid(x, y, z, 1, 1, 1, Color.BLUE, v);
	}

}
