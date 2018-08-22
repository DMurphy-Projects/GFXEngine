package GxEngine3D.Animation;

import DebugTools.TextOutput;
import GxEngine3D.Animation.Routines.IRoutine;
import GxEngine3D.Controller.GXTickEvent;
import GxEngine3D.Controller.ITickListener;
import Shapes.IManipulable;

import java.util.ArrayList;

public class Animator implements ITickListener {

    ArrayList<IManipulable> objects = new ArrayList<>();
    ArrayList<IRoutine> routines = new ArrayList<>();
    int rPos = 0;

    boolean wait = false;
    int delay = 50;

    public void add(IManipulable o)
    {
        objects.add(o);
    }
    public void add(IRoutine r)
    {
        routines.add(r);
    }

    public void setDelay(int d)
    {
        if (delay > 1) {
            delay = d;
        }
        else {
            TextOutput.println("Delay Cannot be set below 1");
        }
    }

    private void delay()
    {
        if (!wait)
        {
            wait = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    wait = false;
                }
            }).start();
        }
    }

    @Override
    public void onTick(GXTickEvent.Type t) {
        if (!wait) {
            if (rPos >= routines.size()) {
                rPos = 0;
                for (IRoutine r:routines)
                {
                    r.reset();
                }
            }
            IRoutine r = routines.get(rPos);
            for (IManipulable m : objects) {
                r.doRoutineStep(m, r.nextStep());
            }
            if (!r.hasNextStep()) {
                rPos++;
            }
            delay();
        }
    }
}
