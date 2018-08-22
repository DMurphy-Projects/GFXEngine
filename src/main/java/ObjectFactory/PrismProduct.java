package ObjectFactory;

import java.awt.Color;

import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;
import Shapes.Prism;

public class PrismProduct implements IProduct{

	@Override
	public String Name() {
		return Prism.getName();
	}

	@Override
	public BaseShape create(double x, double y, double z) {
		Prism p = new Prism(Color.BLUE);
		p.translate(x, y, z);
		return p;
	}

}
