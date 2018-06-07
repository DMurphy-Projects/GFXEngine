package Shapes.Shape2D;

import java.awt.*;

/**
 * Created by Dean on 07/01/17.
 */
public class Sqaure extends NSidedPolygon{

    public Sqaure(Color c) {
        super(c);
    }

    @Override
    protected double getSides() {
        return 4;
    }
}
