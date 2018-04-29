package ObjectFactory;

import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;
import Shapes.FakeSphere;

import java.awt.*;

/**
 * Created by Dean on 28/12/16.
 */
public class FakeSphereProduct implements IProduct {
    @Override
    public String Name() {
        return "Fake Sphere";
    }

    @Override
    public BaseShape create(double x, double y, double z) {
        return new FakeSphere(x, y, z, 1, Color.red);
    }
}
