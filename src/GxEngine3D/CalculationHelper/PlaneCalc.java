package GxEngine3D.CalculationHelper;

import GxEngine3D.Model.Plane;
import GxEngine3D.Model.Polygon3D;
import GxEngine3D.Model.RefPoint3D;
import GxEngine3D.Model.Vector;

public class PlaneCalc {

    static double epsilon = 1e-5;
    public static boolean insidePolygon(Polygon3D poly, Plane plane, double[] point)
    {
        RefPoint3D[] shape = poly.getShape();
        //is on plane
        double side = PlaneCalc.whichSide(point, plane.getNV().toArray(), plane.getP());
        if (side >= -epsilon && side <= epsilon)
        {
            int i, j, c=0, t=shape.length;
            for (i = 0, j = shape.length-1; i < shape.length; j = i++) {
                double[] v1 = VectorCalc.sub_v3v3(shape[i].toArray(), shape[j].toArray());
                double[] v2 = VectorCalc.sub_v3v3(shape[i].toArray(), point);
                double dot = VectorCalc.dot_v3v3(v1, v2);
                if (dot < -epsilon)
                {
                    c--;
                }
                else if (dot > epsilon)
                {
                    c++;
                }
                else
                {
                    //if "0" it shouldn't impact the result
                    t--;
                }
            }

            if (Math.abs(c) == t)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        //when used by the scene, we already know the point is on the plane
        return false;
    }

    public static double whichSide(double[] point, double[] planeNormal, double[] planePoint)
    {
        double[] v = VectorCalc.sub_v3v3(planePoint, point);
        double dot = VectorCalc.dot_v3v3(v, planeNormal);
        return dot;
    }

    public static int whichSide(RefPoint3D[] testPoints, double[] planeNormal, double[] planePoint)
    {
        int pos = 0, neg = 0;
        for (RefPoint3D testPoint:testPoints) {
            double dot = whichSide(testPoint.toArray(), planeNormal, planePoint);
            if (dot > 0)
            {
                pos++;
            }
            else if(dot < 0)
            {
                neg++;
            }
            if (pos > 0 && neg > 0)
            {
                return 0;
            }
        }
        if (pos > 0)
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }

    //gets two planes whose intersection is the line
    public static Plane[] getFullPlaneFromLine(double[] p1, double[] p2)
    {
        double[] v1 = VectorCalc.sub_v3v3(p1, p2);

        double[] noise01 = new double[]{1, 1, 0};
        double[] noise02 = new double[]{0, 1, 1};
        if (VectorCalc.v3_v3_equals(v1, noise01))
        {
            noise01 = new double[]{1, 0, 1};
        }
        else if(VectorCalc.v3_v3_equals(v1, noise02))
        {
            noise02 = new double[]{1, 0, 1};
        }
        Plane plane1 = new Plane(new Vector(v1), new Vector(VectorCalc.add_v3v3(v1, noise01)), p1);
        Plane plane2 = new Plane(new Vector(v1), new Vector(VectorCalc.add_v3v3(v1, noise02)), p1);
        return new Plane[]{plane1, plane2};
    }

    //gets a plane which the line is on
    public static Plane getHalfPlaneFromLine(double[] p1, double[] p2)
    {
        double[] v1 = VectorCalc.sub_v3v3(p1, p2);
        double[] noise01 = new double[]{1, 1, 0};
        if (VectorCalc.v3_v3_equals(v1, noise01))
        {
            noise01 = new double[]{1, 0, 1};
        }
        Plane plane1 = new Plane(new Vector(v1), new Vector(VectorCalc.add_v3v3(v1, noise01)), p1);
        return plane1;
    }
}
