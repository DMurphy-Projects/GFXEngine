package GxEngine3D.Controller;

import java.util.ArrayList;

/**
 * Created by Dean on 28/12/16.
 */
public class GXTickEvent {
    ArrayList<ITickListener> listeners = new ArrayList<ITickListener>();
    public void add(ITickListener i)
    {
        listeners.add(i);
    }
    public void notifyTick()
    {
        for (ITickListener l:listeners) {
            l.onTick();
        }
    }
}
