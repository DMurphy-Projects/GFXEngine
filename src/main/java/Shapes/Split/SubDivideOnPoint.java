package Shapes.Split;

import GxEngine3D.Model.Polygon3D;
import GxEngine3D.Model.RefPoint3D;
import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;

import java.awt.*;
import java.util.ArrayList;

/**
 * Created by Dean on 08/01/17.
 */
public class SubDivideOnPoint extends BaseSplit {
    int i0=0, i1 = 1, i2=2;
    @Override
    public void split(double maxSize, ArrayList<Polygon3D> polys, Color c, BaseShape bTo) {
        super.split(maxSize, polys, c, bTo);
        for (int i=0;i<polys.size();i++) {
            Polygon3D polygonToSplit = polys.get(i);
            RefPoint3D[] sPoints = polygonToSplit.getShape();

            if(sPoints.length == 3)
            {
                if (shouldSplit(maxSize, sPoints)) {
                    polys.remove(i);
                    RefPoint3D split = new RefPoint3D((sPoints[i1].X() + sPoints[i2].X()) / 2,
                            (sPoints[i1].Y() + sPoints[i2].Y()) / 2,
                            (sPoints[i1].Z() + sPoints[i2].Z()) / 2);
                    i++;
                    polys.add(0, new Polygon3D(new RefPoint3D[]{sPoints[i0], sPoints[i1], split}, c, bTo));
                    polys.add(0, new Polygon3D(new RefPoint3D[]{sPoints[i0], split, sPoints[i2]}, c, bTo));
                }
            }
        }
        if (needMore) {
            int i=i0;
            i0 = i1;
            i1 = i2;
            i2 = i;
            split(maxSize, polys, c, bTo);
        }
    }
}
