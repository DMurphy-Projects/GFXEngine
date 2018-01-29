package ObjectFactory;

import java.awt.Color;

import GxEngine3D.ViewHandler;
import Shapes.BaseShape;
import Shapes.Shape2D.Circle;

public class CircleProduct implements IProduct{

	@Override
	public String Name() {
		return Circle.getName();
	}

	@Override
	public BaseShape create(double x, double y, double z, ViewHandler v) {
		return new Circle(x, y, z, 1, Color.BLUE, v);
	}

}
