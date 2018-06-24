package GxEngine3D.Animation.Routines;

import GxEngine3D.Animation.Routines.BaseRoutine;
import Shapes.IManipulable;

public class AnchorRoutine extends BaseRoutine {

    double[] anchor, offset;

    public AnchorRoutine(double[] a, double[] o)
    {
        this.step = 1;
        anchor = a;
        offset = o;
    }

    @Override
    public void doRoutineStep(IManipulable s, double pos) {
        s.setAnchor(anchor);
        s.translate(offset[0], offset[1], offset[2]);
    }
}
