package ObjectFactory;

import GxEngine3D.ViewHandler;
import Shapes.BaseShape;

public interface IProduct {

	public String Name();
	public BaseShape create(double x, double y, double z, ViewHandler v);
}
