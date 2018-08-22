package GxEngine3D.Animation.Routines;

public abstract class BaseRoutine implements IRoutine {

    protected double pos = 0, maxPos = 1;
    protected double step = 0.01;

    @Override
    public boolean hasNextStep() {
        return pos < maxPos;
    }

    @Override
    public double nextStep() {
        pos += step;
        if (pos > maxPos)
        {
            pos = maxPos;
        }
        return pos;
    }

    @Override
    public void reset() {
        pos = 0;
    }
}
