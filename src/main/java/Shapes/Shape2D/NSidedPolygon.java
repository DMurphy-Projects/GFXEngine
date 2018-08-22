package Shapes.Shape2D;

import GxEngine3D.Model.RefPoint3D;
import Shapes.BaseShape;

import java.awt.*;

//TODO normalise relative shape to 0-1
public class NSidedPolygon extends BaseShape {
    public NSidedPolygon(Color c) {
        super(c);
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
            double _x = 0.5+Math.cos(i),
                    _y = 0.5+Math.sin(i);
            addPoint(new double[]{_x, _y, 0});
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
        for (int i = 0; i< points.size(); i++)
        {
            poly[i] = points.get(i);
        }
        return poly;
    }
}
