package GxEngine3D.Camera;

/**
 * Created by Dean on 20/03/17.
 */
public interface ICameraEventListener {
    void onLook(double h, double v);
    void onMove(double x, double y, double z);
}
