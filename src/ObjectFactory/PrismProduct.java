package ObjectFactory;

import java.awt.Color;

import GxEngine3D.ViewHandler;
import Shapes.BaseShape;
import Shapes.Prism;

public class PrismProduct implements IProduct{

	@Override
	public String Name() {
		return Prism.getName();
	}

	@Override
	public BaseShape create(double x, double y, double z, ViewHandler v) {
		return new Prism(x, y, z, 1, 1, 1, Color.BLUE, v);
	}

}
