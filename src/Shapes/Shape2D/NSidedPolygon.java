package Shapes.Shape2D;

import GxEngine3D.RefPoint3D;
import GxEngine3D.ViewHandler;
import Shapes.BaseShape;

import java.awt.*;

public class NSidedPolygon extends BaseShape {
    public NSidedPolygon(double x, double y, double z, double rad, Color c, ViewHandler v) {
        super(x, y, z, rad, rad, rad, c, v);
    }

    protected double getSides()
    {
        return 3;
    }

    @Override
    protected void createShape() {
        double a = (Math.PI*2)/getSides();
        //System.out.println(xS+" "+yS);
        for (double i=0;i<Math.PI*2;i+=a)
        {
            double _x = x+(Math.cos(i)*(width/2)),
                    _y = y+(Math.sin(i)*(height/2));
            points.add(new RefPoint3D(_x, _y, z));
            //points.add(new RefPoint3D(_x, _y, z+0.0000001));

        }
        add(0, 1);
    }
    private void add(int offset, int split)
    {
        RefPoint3D[] p = new RefPoint3D[points.size()/split];
        for (int i=offset;i<points.size();i+=split)
        {
            p[(i-offset)/split] = points.get(i);
        }
        add(p);
    }

}
