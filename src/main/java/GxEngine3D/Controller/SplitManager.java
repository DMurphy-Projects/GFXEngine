package GxEngine3D.Controller;

import Shapes.BaseShape;

import java.awt.*;

/**
 * Created by Dean on 31/12/16.
 */
public class SplitManager {
    private boolean needUpdate = false;
    private double maxSize = 1000;
    public SplitManager(double size)
    {
        maxSize = size;
    }

    //override to add functionality
    public void updateSplitting()
    {
        needUpdate = false;
    }
    public void scheduleSplit()
    {
        if (!needUpdate && maxSize>0)
            needUpdate = true;
    }
    public void split(BaseShape s)
    {
        s.split(maxSize);
    }
    public double Size()
    {
        return maxSize;
    }
    public Boolean NeedSplit()
    {
        return needUpdate;
    }
}
