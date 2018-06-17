package Shapes.Plane;

import GxEngine3D.Model.RefPoint3D;

import java.awt.*;

public class InfiniteGrid extends InfinitePlane {
    public InfiniteGrid(Color c) {
        super(c);
    }

    @Override
    protected void createShape() {
        int diameter = 40;

        for (int w=0;w<=diameter;w++)
        {
            for (int h=0;h<=diameter;h++)
            {
                addPoint(new double[]{(2*(double)(w)/diameter)-1, 0, (2*(double)(h)/diameter)-1});
                if (w > 0 && h > 0)
                {
                    addPoly(new RefPoint3D[]{
                            points.get(getPosition(w-1, h-1, diameter+1)),
                            points.get(getPosition(w-1, h, diameter+1)),
                            points.get(getPosition(w, h, diameter+1)),
                            points.get(getPosition(w, h-1, diameter+1))}, c);
                }
            }
        }

        absoluteScale(diameter/2, 1, diameter/2);
    }

    private int getPosition(int w, int h, int size)
    {
        int p = (w*size) + h;
        return p;
    }
}
