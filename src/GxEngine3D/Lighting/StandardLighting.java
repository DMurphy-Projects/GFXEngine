package GxEngine3D.Lighting;

import GxEngine3D.Camera.Camera;
import GxEngine3D.Model.Plane;
import GxEngine3D.Model.Vector;

/**
 * Created by Dean on 29/12/16.
 */
public class StandardLighting implements ILightingStrategy {
    @Override
    public double doLighting(Light l, Plane lightingPlane, Camera c) {
        //get which side of the lightingPlane te camera is on
        //light vector is direction towards plane
        //so finding which side the camera is on tells us if the light is behind or infront of the plan we're
        //looking at
        Vector nVector = lightingPlane.getNV(c.position());

        Vector lightVector = l.getLightVector(lightingPlane.getP());
        double angle = Math.acos(nVector.dot(lightVector));

        //DrawablePolygon.lighting = 0.2 + 1 - Math.sqrt(angle / Math.PI);
        double lighting = (120 + (Math.cos(angle) * 90)) / 255;
        //clamp lighting just in case

        return lighting;
    }
}
