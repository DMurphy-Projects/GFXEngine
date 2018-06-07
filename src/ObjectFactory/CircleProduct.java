package ObjectFactory;

import java.awt.Color;

import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;
import Shapes.Shape2D.Circle;

public class CircleProduct implements IProduct{

	@Override
	public String Name() {
		return Circle.getName();
	}

	@Override
	public BaseShape create(double x, double y, double z) {
		Circle c = new Circle(Color.BLUE);
		c.translate(x, y, z);
		return c;
	}

}
