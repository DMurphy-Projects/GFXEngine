package Shapes;

import java.util.ArrayList;

import GxEngine3D.Model.Polygon3D;
import GxEngine3D.Model.RefPoint3D;

public interface IShape {
	ArrayList<Polygon3D> getShape();
	ArrayList<RefPoint3D> getPoints();
	void update();
}
