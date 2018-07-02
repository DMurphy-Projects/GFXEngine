package Shapes.Plane;

import GxEngine3D.Model.RefPoint3D;

import java.awt.*;

public class InfiniteGrid extends InfinitePlane {
    int diameter;
    public InfiniteGrid(Color c, Integer i) {
        super(c);
        diameter = i;
    }

    @Override
    protected void createShape() {
        if (diameter == 1)
        {
            addPoint(new double[]{0, 0, 0});
            addPoint(new double[]{0, 0, 1});
            addPoint(new double[]{1, 0, 1});
            addPoint(new double[]{1, 0, 0});

            addPoly(new RefPoint3D[]{
                    points.get(0),
                    points.get(1),
                    points.get(2),
                    points.get(3),
            }, c);
        }
        else {
            for (int w = 0; w <= diameter; w++) {
                for (int h = 0; h <= diameter; h++) {
                    addPoint(new double[]{(2 * (double) (w) / diameter) - 1, 0, (2 * (double) (h) / diameter) - 1});
                    if (w > 0 && h > 0) {
                        addPoly(new RefPoint3D[]{
                                points.get(getPosition(w - 1, h - 1, diameter + 1)),
                                points.get(getPosition(w - 1, h, diameter + 1)),
                                points.get(getPosition(w, h, diameter + 1)),
                                points.get(getPosition(w, h - 1, diameter + 1))}, c);
                    }
                }
            }
            //we need to scale the grid by the diameter and since the grid is laid out in the range(-1, 1), the scale must be halved
            absoluteScale(((double)diameter)/2, 1, ((double)diameter)/2);
        }

    }

    private int getPosition(int w, int h, int size)
    {
        int p = (w*size) + h;
        return p;
    }
}
