package GxEngine3D.Animation.Routines;

import GxEngine3D.Animation.Routines.BaseRoutine;
import GxEngine3D.CalculationHelper.VectorCalc;
import Shapes.IManipulable;

public class RotationRoutine extends BaseRoutine {
    double[] rotate = new double[]{Math.PI/2, 0, 0};
    double[] prevR = new double[3];

    public RotationRoutine(double[] r)
    {
        this.step = 0.1;
        rotate = r;
    }

    @Override
    public void doRoutineStep(IManipulable s, double pos) {
        double[] m = VectorCalc.mul_v_d(rotate, pos);
        double [] n = VectorCalc.sub(m, prevR);
        prevR = m;
        s.rotate(n[0], n[1], n[2]);
    }

    @Override
    public void reset() {
        super.reset();
        prevR = new double[3];
    }
}
