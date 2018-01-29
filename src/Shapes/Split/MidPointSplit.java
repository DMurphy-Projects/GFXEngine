package Shapes.Split;

import GxEngine3D.Polygon3D;
import GxEngine3D.RefPoint3D;
import GxEngine3D.ViewHandler;
import Shapes.BaseShape;

import java.awt.*;
import java.util.ArrayList;

/**
 * Created by Dean on 20/03/17.
 */
public class MidPointSplit extends BaseSplit
{
    @Override
    public void split(double maxSize, ArrayList<Polygon3D> polys, Color c, ViewHandler vH, BaseShape bTo) {
        super.split(maxSize, polys, c, vH, bTo);
        for (int i=0;i<polys.size();i++) {
            Polygon3D polygonToSplit = polys.get(i);
            RefPoint3D[] sPoints = polygonToSplit.getShape();

            if (shouldSplit(maxSize, sPoints)) {

                polys.remove(i);

                double x = 0, y = 0, z = 0;
                for (RefPoint3D p : sPoints) {
                    x += p.X();
                    y += p.Y();
                    z += p.Z();
                }
                RefPoint3D middle = new RefPoint3D(x / sPoints.length, y / sPoints.length, z / sPoints.length);

                for (int ii = 1; ii <= sPoints.length - 1; ii++) {
                    RefPoint3D p1 = sPoints[ii];
                    RefPoint3D p2 = sPoints[ii - 1];
                    polys.add(0, new Polygon3D(new RefPoint3D[]{p1, p2, middle}, c, vH, bTo));
                }
                polys.add(0, new Polygon3D(new RefPoint3D[]{sPoints[0], sPoints[sPoints.length - 1], middle}, c, vH, bTo));
            }
        }
        if (needMore)
        {
            split(maxSize, polys, c, vH, bTo);
        }
    }
}
