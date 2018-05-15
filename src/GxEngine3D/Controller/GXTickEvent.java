package GxEngine3D.Controller;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dean on 28/12/16.
 */
public class GXTickEvent {

    public enum Type
    {
        PreTick,
        Tick,
        PostTick
    }

    ArrayList<ITickListener> preListeners = new ArrayList<ITickListener>();
    ArrayList<ITickListener> listeners = new ArrayList<ITickListener>();
    ArrayList<ITickListener> postListeners = new ArrayList<ITickListener>();

    public void add(ITickListener i)
    {
        listeners.add(i);
    }
    public void addPreListener(ITickListener i)
    {
        preListeners.add(i);
    }
    public void addPostListener(ITickListener i)
    {
        postListeners.add(i);
    }

    public void notifyTick()
    {
        notify(listeners, Type.Tick);
    }
    public void notifyPreTick()
    {
        notify(preListeners, Type.PreTick);
    }
    public void notifyPostTick()
    {
        notify(postListeners, Type.PostTick);
    }

    private void notify(List<ITickListener> list, Type t)
    {
        for (ITickListener l:list) {
            l.onTick(t);
        }
    }
}
