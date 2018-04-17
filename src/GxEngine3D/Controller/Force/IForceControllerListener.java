package GxEngine3D.Controller.Force;

import Shapes.ForceShape;

public interface IForceControllerListener {
    void onObjectAdded(ForceShape fs);
    void onObjectRemoved(ForceShape fs);
}
