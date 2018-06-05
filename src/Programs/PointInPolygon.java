package Programs;

import GxEngine3D.CalculationHelper.PlaneCalc;
import GxEngine3D.CalculationHelper.VectorCalc;
import GxEngine3D.Model.Plane;
import GxEngine3D.Model.Polygon3D;
import GxEngine3D.Model.RefPoint3D;
import Shapes.BaseShape;

public class PointInPolygon {

    public static void main(String[] args)
    {
        RefPoint3D[] poly = new RefPoint3D[]{
                new RefPoint3D(0 ,0, 0),
                new RefPoint3D(1 ,0, 0),
                new RefPoint3D(1 ,1, 0),
                new RefPoint3D(0 ,1, 0),
        };

        RefPoint3D point = new RefPoint3D(0, 0, 0);

        //TODO
        //at some point i should really fix this mess
        Plane plane = new Plane(new Polygon3D(poly, null, new BaseShape(0, 0, 0, 0, 0, 0, null) {
            @Override
            protected void createShape() {
                points.add(new RefPoint3D(0, 0, 0));
            }
        }));

        //is on plane
        double side = PlaneCalc.whichSide(point.toArray(), plane.getNV().toArray(), plane.getP());
        if (side == 0)
        {
            int i, j, c=0;
                for (i = 0, j = poly.length-1; i < poly.length; j = i++) {
                double[] v1 = VectorCalc.sub(poly[i].toArray(), poly[j].toArray());
                double[] v2 = VectorCalc.sub(poly[i].toArray(), point.toArray());
                double dot = VectorCalc.dot(v1, v2);
                c = (dot>=0)?c+1:c-1;
            }
            if (Math.abs(c) == poly.length)
            {
                System.out.println("Inside");
            }
            else
            {
                System.out.println("Outside");
            }
        }
        else
        {
            System.out.println("Not on plane");
        }
    }
}
