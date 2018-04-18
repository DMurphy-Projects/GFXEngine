package GxEngine3D.Lighting;

import GxEngine3D.CalculationHelper.VectorCalc;
import GxEngine3D.Model.Vector;
import Shapes.Shape2D.Line;

public class Light {

	double[] lightPos;

	//brightness is how far the light can travel before it starts to decay
	int brightness;
	
	public Line line;
	
	public Light(double x, double y, double z, int b, Line l) {
		lightPos = new double[] { x, y, z };
		line = l;
		brightness = b;
	}
	
	public void updateLighting()
	{
		line.setEnd(new double[]{lightPos[0], lightPos[1], lightPos[2]});
	}
	
	public double[] getLightVector(double[] from)
	{
		return VectorCalc.norm_v3(VectorCalc.sub_v3v3(from, lightPos));
	}
	
	public int getBrightness()
	{
		return brightness;
	}

}
