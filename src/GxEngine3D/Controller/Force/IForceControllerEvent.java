package GxEngine3D.Controller.Force;

import Shapes.ForceShape;

public interface IForceControllerEvent {
    void notifyAdded(ForceShape fs);
    void notifyRemoved(ForceShape fs);
    void addListener(IForceControllerListener l);
    void removeListener(IForceControllerListener l);
}
