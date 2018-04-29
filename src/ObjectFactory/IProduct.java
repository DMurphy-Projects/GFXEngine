package ObjectFactory;

import Shapes.BaseShape;

public interface IProduct {

	String Name();
	BaseShape create(double x, double y, double z);
}
