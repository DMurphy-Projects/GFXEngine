package GxEngine3D.Animation.Routines;

import Shapes.IManipulable;

public interface IRoutine
{
    boolean hasNextStep();
    double nextStep();
    void reset();
    void doRoutineStep(IManipulable s, double pos);
}
