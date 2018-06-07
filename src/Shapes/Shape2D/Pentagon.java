package Shapes.Shape2D;

import java.awt.*;

/**
 * Created by Dean on 31/12/16.
 */
public class Pentagon extends NSidedPolygon {
    public Pentagon(Color c) {
        super(c);
    }

    @Override
    protected double getSides() {
        return 5;
    }
}
