package GxEngine3D.Lighting;

import GxEngine3D.Helper.VectorCalc;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Model.Plane;

//lacks the subtlety of previous methods but is most correct at this point
public class AltLighting implements ILightingStrategy {
    @Override
    public double doLighting(Light l, Plane p, Camera c) {

        double[] lightV = VectorCalc.mul_v_d(l.getLightVector(p.getP()), -1);
        double[] surface = VectorCalc.norm(p.getNV().toArray());
        double flip = VectorCalc.dot(VectorCalc.norm(VectorCalc.sub(p.getP(), c.From())), surface);
        if (flip > 0)
        {
            surface = VectorCalc.mul_v_d(surface, -1);
        }
        //if the vector of the surface normal equals the light vector, means that the light is behind the plane and the camera is in front
        //meaning that the side viewed would be dark
        if (VectorCalc.dot(surface, lightV) > 0) {
            return 0.33;
        }
        else {
            return 1;
        }
    }
}
