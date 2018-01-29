package GxEngine3D.Lighting;

import GxEngine3D.CalculationHelper.RotationCalc;
import Shapes.Shape2D.Line;

/**
 * Created by Dean on 07/01/17.
 */
public class OrbitingLight extends Light {
    double angle = 0.01;//how much it moves per update
    double[] origin;
    public OrbitingLight(double x, double y, double z, int rad, double speed, int b, Line l) {
        super(x, y, z, b, l);
        origin = new double[]{x, y, z};
        lightPos = new double[]{x+rad, y, z};
        angle = speed;
    }

    @Override
    public void updateLighting() {
        double[] rot = RotationCalc.rotate(lightPos, origin, angle);
        lightPos = new double[]{rot[0], rot[1], lightPos[2]};
        super.updateLighting();
    }
}
