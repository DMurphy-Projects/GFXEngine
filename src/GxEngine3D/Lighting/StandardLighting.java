package GxEngine3D.Lighting;

import GxEngine3D.Helper.DistanceCalc;
import GxEngine3D.Helper.VectorCalc;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Model.Plane;

/**
 * Created by Dean on 29/12/16.
 */
public class StandardLighting implements ILightingStrategy {
    //this seems to work but may be mathematically incorrect
    @Override
    public double doLighting(Light l, Plane lightingPlane, Camera c) {
        //get which side of the lightingPlane te camera is on
        //light vector is direction towards plane
        //so finding which side the camera is on tells us if the light is behind or infront of the plan we're
        //looking at
        double[] nVector = lightingPlane.getNV(c.getPosition()).toArray();
        nVector = VectorCalc.norm(nVector);

        double[] lightVector = l.getLightVector(lightingPlane.getP());
        double lighting = VectorCalc.dot(nVector, lightVector);
        //get a factor to modify the lightning value with
        //uses inverse square law
        double dist = DistanceCalc.getDistanceNoRoot(l.lightPos, lightingPlane.getP());
        double factor = 1d / Math.pow(dist / l.getBrightness() , 2);
        //the factor will make the bright brighter and the dark darker
        lighting *= factor;
        //if its brighter than it can get, then clamp to max brightness
        if (lighting > 1)
            lighting = 1;
        else if (lighting < -1)
            lighting = -1;
        return 0.5d+(lighting/2);
    }
}
