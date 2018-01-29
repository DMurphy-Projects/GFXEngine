package GxEngine3D.Camera;

/**
 * Created by Dean on 20/03/17.
 */
public interface ICameraEvent {
    void notifyMove();
    void notifyLook();
    void add(ICameraEventListener e);
    void remove(ICameraEvent e);
}
