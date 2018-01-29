package Shapes.Split;

import GxEngine3D.CalculationHelper.VectorCalc;
import GxEngine3D.Polygon3D;
import GxEngine3D.RefPoint3D;
import GxEngine3D.Vector;
import GxEngine3D.ViewHandler;
import Shapes.BaseShape;

import java.awt.*;
import java.util.ArrayList;

/**
 * Created by Dean on 20/03/17.
 */
public class BaseSplit implements ISplitStrategy
{
    boolean needMore = false;

    public boolean shouldSplit(double maxSize, RefPoint3D[] sPoints) {
        Vector ab = new Vector(VectorCalc.sub_v3v3(sPoints[1].toArray(), sPoints[0].toArray()), false);
        Vector ac = new Vector(VectorCalc.sub_v3v3(sPoints[2].toArray(), sPoints[0].toArray()), false);
        Vector cross = ab.crossProduct(ac, false);
        cross = cross.multiply(cross, false);
        double area = 0.5 * Math.sqrt(cross.X()+cross.Y()+cross.Z());
        if (area < 0)area*=-1;

        boolean should = area >= maxSize;
        if (should)
        {
            needMore = true;
        }
        return should;
    }

    @Override
    public void split(double maxSize, ArrayList<Polygon3D> polys, Color c, ViewHandler vH, BaseShape bTo) {
        needMore = false;
        //this class is to provide easy methods for determining properties of shape
        //implementation of split is not one
    }
}
