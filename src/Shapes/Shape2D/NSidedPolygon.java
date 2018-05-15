package Shapes.Shape2D;

import GxEngine3D.Model.RefPoint3D;
import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;

import java.awt.*;

public class NSidedPolygon extends BaseShape {
    public NSidedPolygon(double x, double y, double z, double rad, Color c) {
        super(x, y, z, rad, rad, rad, c);
    }

    protected double getSides()
    {
        return 3;
    }

    @Override
    protected void createShape() {
        double a = (Math.PI*2)/getSides();
        int index = 0;
        for (double i=0;i<Math.PI*2;i+=a)
        {
            double _x = x+(Math.cos(i)*(width/2)),
                    _y = y+(Math.sin(i)*(height/2));
            points.add(new RefPoint3D(_x, _y, z));
            if (index>0)
            {
                addEdge(new RefPoint3D[]{points.get(index), points.get(index-1)});
            }
            index++;
        }
        addEdge(new RefPoint3D[]{points.get(points.size()-1), points.get(0)});
        addPoly(getPoly(), c);
    }

    private RefPoint3D[] getPoly()
    {
        RefPoint3D[] poly = new RefPoint3D[points.size()];
        for (int i=0;i<points.size();i++)
        {
            poly[i] = points.get(i);
        }
        return poly;
    }
}
