package Shapes;

import GxEngine3D.CalculationHelper.DistanceCalc;
import GxEngine3D.CalculationHelper.VectorCalc;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Lighting.ILightingStrategy;
import GxEngine3D.Lighting.Light;
import GxEngine3D.Model.Plane;
import GxEngine3D.Model.RefPoint3D;
import GxEngine3D.Model.Vector;
import GxEngine3D.View.ViewHandler;
import Shapes.Shape2D.Circle;

import java.awt.*;

/**
 * Created by Dean on 28/12/16.
 */
public class FakeSphere extends Circle {
    public FakeSphere(double x, double y, double z, double rad, Color c) {
        super(x, y, z, rad, c);
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
        double offset = length/2;//technically l==h==w
        points.add(new RefPoint3D(x-width/2, y-height/2, z+offset));
        points.add(new RefPoint3D(x-width/2, y+height/2, z+offset));
        points.add(new RefPoint3D(x+width/2, y-height/2, z+offset));
        points.add(new RefPoint3D(x+width/2, y+height/2, z+offset));
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
