package Shapes.Shape2D;

import GxEngine3D.Intersection3D.IIntersection3DStrategy;
import GxEngine3D.Intersection3D.TriangleSideIntersection;
import GxEngine3D.View.ViewHandler;

import java.awt.*;

/**
 * Created by Dean on 07/01/17.
 */
public class Sqaure extends NSidedPolygon{

    IIntersection3DStrategy finder = new TriangleSideIntersection();

    public Sqaure(double x, double y, double z, double rad, Color c) {
        super(x, y, z, rad, c);
    }

    @Override
    protected double getSides() {
        return 4;
    }
}
