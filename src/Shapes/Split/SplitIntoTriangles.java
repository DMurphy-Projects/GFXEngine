package Shapes.Split;

import GxEngine3D.Polygon3D;
import GxEngine3D.RefPoint3D;
import GxEngine3D.ViewHandler;
import Shapes.BaseShape;

import java.awt.*;
import java.util.ArrayList;

/**
 * Created by Dean on 08/01/17.
 */
public class SplitIntoTriangles implements ISplitStrategy {
    //turns any polygon into triangles
    @Override
    public void split(double maxSize, ArrayList<Polygon3D> polys, Color c, ViewHandler vH, BaseShape bTo) {
        for (int i=0;i<polys.size();i++) {
            Polygon3D polygonToSplit = polys.get(i);
            RefPoint3D[] sPoints = polygonToSplit.getShape();

            //easy split
            if (sPoints.length > 3) {
                polys.remove(i);
                RefPoint3D[] newShape;
                int totalPoints = sPoints.length;
                int start = 0, end = start + 2;
                while (totalPoints > 2) {
                    newShape = new RefPoint3D[3];
                    newShape[0] = sPoints[0];
                    for (int ii = 1; ii <= 2; ii++) {
                        int index = ii + start;
                        if (index >= sPoints.length)
                            index -= sPoints.length;
                        newShape[ii] = sPoints[index];
                    }
                    polys.add(0, new Polygon3D(newShape, c, vH, bTo));
                    //i++;//due to adding at 0
                    if (start > 0) i++;
                    totalPoints--;
                    start += 1;
                    end += 1;
                }
            }
        }
    }
}
