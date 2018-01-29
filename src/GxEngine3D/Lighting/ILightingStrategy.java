package GxEngine3D.Lighting;

import GxEngine3D.Camera.Camera;
import GxEngine3D.Plane;

/**
 * Created by Dean on 29/12/16.
 */
public interface ILightingStrategy {
    public double doLighting(Light l, Plane p, Camera c);
}
