package GxEngine3D.Animation.Routines;

import GxEngine3D.Helper.CastingHelper;
import Shapes.IManipulable;

public class AnchorRoutine extends BaseRoutine {

    double[] anchor, offset;

    public AnchorRoutine(Double[] a, Double[] o)
    {
        this.step = 1;
        anchor = CastingHelper.convert(a);
        offset = CastingHelper.convert(o);
    }

    @Override
    public void doRoutineStep(IManipulable s, double pos) {
        s.setAnchor(anchor);
        s.translate(offset[0], offset[1], offset[2]);
    }
}
