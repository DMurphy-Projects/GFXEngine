package GxEngine3D.Camera;

import DebugTools.TextOutput;
import GxEngine3D.CalculationHelper.DistanceCalc;
import GxEngine3D.CalculationHelper.VectorCalc;
import GxEngine3D.Controller.ITickListener;
import Shapes.BaseShape;

public class OrbitingCamera extends Camera implements ITickListener{

    double radius;
    BaseShape anchor;

    public OrbitingCamera(double x, double y, double z, double rad, BaseShape a) {
        super(x, y, z);
        radius = rad*rad;
        anchor = a;
        moveSpeed = 0.5;
    }

    @Override
    public void onTick() {
        double[] viewVector = VectorCalc.norm_v3(VectorCalc.sub_v3v3(viewTo, viewFrom));
        double[] sideVector = VectorCalc.norm_v3(VectorCalc.cross(viewVector, new double[]{0, 0, 1}));

        double xMove = 0, yMove = 0, zMove = 0;

        xMove += sideVector[0];
        yMove += sideVector[1];
        zMove += sideVector[2];

        double d = DistanceCalc.getDistance(viewFrom, anchor.findCentre());
        double dif = d-radius;
        xMove += viewVector[0]*dif;
        yMove += viewVector[1]*dif;
        zMove += viewVector[2]*dif;

        MoveTo(viewFrom[0] + xMove * moveSpeed, viewFrom[1] + yMove
                * moveSpeed, viewFrom[2] + zMove * moveSpeed);

        lookAt(anchor);
    }
}
