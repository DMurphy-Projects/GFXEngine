package GxEngine3D.Animation.Routines;

import GxEngine3D.Helper.Maths.VectorCalc;
import Shapes.IManipulable;

public class TranslationRoutine extends BaseRoutine {
    double[] move = new double[]{0, 1, 0};
    double[] prevM = new double[3];


    public TranslationRoutine(double[] t)
    {
        move = t;
    }
    @Override
    public void doRoutineStep(IManipulable s, double pos) {
        double[] m = VectorCalc.mul_v_d(move, pos);
        double [] n = VectorCalc.sub(m, prevM);
        prevM = m;
        s.translate(n[0], n[1], n[2]);
    }

    @Override
    public void reset() {
        super.reset();
        prevM = new double[3];
    }
}
