package Shapes.Shape2D;

import GxEngine3D.View.ViewHandler;

import java.awt.*;

/**
 * Created by Dean on 31/12/16.
 */
public class Pentagon extends NSidedPolygon {
    public Pentagon(double x, double y, double z, double rad, Color c, ViewHandler v) {
        super(x, y, z, rad, c, v);
    }

    @Override
    protected double getSides() {
        return 5;
    }
}
