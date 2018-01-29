package Shapes;

import GxEngine3D.*;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Lighting.ILightingStrategy;
import GxEngine3D.Lighting.Light;
import Shapes.Shape2D.Circle;

import java.awt.*;

/**
 * Created by Dean on 28/12/16.
 */
public class FakeSphere extends Circle {
    public FakeSphere(double x, double y, double z, double rad, Color c, final ViewHandler v) {
        super(x, y, z, rad, c, v);
        lighting = new ILightingStrategy() {
            @Override
            public double doLighting(Light l, Plane p, Camera c) {
                //lighting on a sphere is equiv to lighting on a plane always facing camera
                p = new Plane(c.W1, c.W2, p.getP());
                Vector lightVector = l.getLightVector(p.getP());

                double angle = Math.acos(p.getNV().dot(lightVector));
                double lighting = (120 + (Math.cos(angle) * 90)) / 255;
                if (lighting > 1)
                    lighting = 1;
                if (lighting < 0)
                    lighting = 0;
                return lighting;
            }
        };
    }

    @Override
    public void split(double maxSize) {

    }

    @Override
    protected void createShape() {
        double offset = length/2;//technically l==h==w
        points.add(new RefPoint3D(x-width/2, y-height/2, z+offset));
        points.add(new RefPoint3D(x-width/2, y+height/2, z+offset));
        points.add(new RefPoint3D(x+width/2, y-height/2, z+offset));
        points.add(new RefPoint3D(x+width/2, y+height/2, z+offset));
        add(new RefPoint3D[]{points.get(0), points.get(1), points.get(3), points.get(2)});
    }

    @Override
    public void draw(Graphics g, Polygon p) {
        Rectangle bound = p.getBounds();
        g.fillOval((int)bound.getX(), (int)bound.getY(), bound.width, bound.width);
    }

    @Override
    public void drawOutlines(Graphics g, Polygon p) {
        //super.drawOutlines(g, p);//will draw real shape, this is fake sphere
        Rectangle bound = p.getBounds();
        g.drawOval((int)bound.getX(), (int)bound.getY(), bound.width, bound.width);
    }

    @Override
    public void drawHighlight(Graphics g, Polygon p) {
    }
}
