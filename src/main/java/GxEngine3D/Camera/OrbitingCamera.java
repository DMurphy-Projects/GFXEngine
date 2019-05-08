package GxEngine3D.Camera;

import GxEngine3D.Helper.Maths.DistanceCalc;
import GxEngine3D.Helper.Maths.VectorCalc;
import GxEngine3D.Controller.GXTickEvent;
import GxEngine3D.Controller.ITickListener;
import Shapes.BaseShape;

public class OrbitingCamera extends Camera implements ITickListener{

    double radius;
    BaseShape anchor;

    public OrbitingCamera(double x, double y, double z, double rad, BaseShape a) {
        super(x, y, z);
        radius = rad*rad;
        anchor = a;
        moveSpeed = 0.125;
    }

    @Override
    public void onTick(GXTickEvent.Type t) {
        double[] viewVector = VectorCalc.norm(getDirection());
        double[] sideVector = VectorCalc.norm(VectorCalc.cross(viewVector, new double[]{0, 1, 0}));

        double[] move = new double[3];

        move = VectorCalc.add(move, sideVector);

        double d = DistanceCalc.getDistance(viewFrom, anchor.findCentre());
        double dif = d-radius;
        move = VectorCalc.add(move, VectorCalc.mul_v_d(viewVector, dif));

        move = VectorCalc.add(viewFrom, VectorCalc.mul_v_d(move, moveSpeed));
        MoveTo(move[0], move[1], move[2]);

        lookAt(anchor);
    }
}
