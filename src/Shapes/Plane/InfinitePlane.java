package Shapes.Plane;

import GxEngine3D.Camera.ICameraEventListener;
import GxEngine3D.Model.RefPoint3D;
import Shapes.BaseShape;

import java.awt.*;

//needs polygon clipping to work correctly
public class InfinitePlane extends BaseShape implements ICameraEventListener{

    public InfinitePlane(Color c) {
        super(c);
    }

    @Override
    protected void createShape() {
        addPoint(new double[]{-1, 0, -1});
        addPoint(new double[]{-1, 0, 1});
        addPoint(new double[]{1, 0, 1});
        addPoint(new double[]{1, 0, -1});

        addPoly(new RefPoint3D[]{
                points.get(0),
                points.get(1),
                points.get(2),
                points.get(3),
                }, c);
    }

    @Override
    public void onLook(double h, double v) {

    }

    @Override
    public void onMove(double x, double y, double z) {
        absoluteTranslate((int)x, (int)this.y, (int)z);
    }
}
