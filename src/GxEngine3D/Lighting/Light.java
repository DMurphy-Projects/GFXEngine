package GxEngine3D.Lighting;

import GxEngine3D.Helper.VectorCalc;
import Shapes.FakeSphere;

public class Light {

	double[] lightPos;

	//brightness is how far the light can travel before it starts to decay
	int brightness;
	FakeSphere light;
	
	public Light(double x, double y, double z, int b, FakeSphere l) {
		lightPos = new double[] { x, y, z };
		light = l;
		brightness = b;
	}
	
	public void updateLighting()
	{
		light.absoluteTranslate(lightPos[0], lightPos[1], lightPos[2]);
	}
	
	public double[] getLightVector(double[] to)
	{
		return VectorCalc.norm(VectorCalc.sub(lightPos, to));
	}
	
	public int getBrightness()
	{
		return brightness;
	}
	public double[] getPosition()
	{
		return lightPos;
	}

}
