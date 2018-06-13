package Shapes;

import GxEngine3D.Camera.Camera;
import GxEngine3D.Lighting.ILightingStrategy;
import GxEngine3D.Lighting.Light;
import GxEngine3D.Model.Plane;
import GxEngine3D.Model.RefPoint3D;
import Shapes.Shape2D.Circle;

import java.awt.*;

/**
 * Created by Dean on 28/12/16.
 */
//TODO fake sphere isn't as robust in the matrix version, needs a better version of fake sphere
public class FakeSphere extends Circle {
    public FakeSphere(Color c) {
        super(c);
        lighting = new ILightingStrategy() {
            @Override
            public double doLighting(Light l, Plane p, Camera c) {
                return 1;
            }
        };
    }

    @Override
    public void split(double maxSize) {

    }

    @Override
    protected void createShape() {
        addPoint(new double[]{0, 0, 0});
        addPoint(new double[]{1, 0, 0});
        addPoint(new double[]{1, 1, 0});
        addPoint(new double[]{0, 1, 0});
        addEdge(new RefPoint3D[]{points.get(0), points.get(1)});
        addEdge(new RefPoint3D[]{points.get(1), points.get(2)});
        addEdge(new RefPoint3D[]{points.get(2), points.get(3)});
        addEdge(new RefPoint3D[]{points.get(3), points.get(0)});
        addPoly(new RefPoint3D[]{points.get(0), points.get(1), points.get(2), points.get(3), }, c);
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
