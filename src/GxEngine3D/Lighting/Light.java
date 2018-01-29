package GxEngine3D.Lighting;

import GxEngine3D.Vector;
import Shapes.Shape2D.Line;

public class Light {

	double[] lightPos;

	int brightness = 5;
	
	public Line line;
	
	public Light(double x, double y, double z, int b, Line l) {
		lightPos = new double[] { x, y, z };
		line = l;
		if (b > 1)
			brightness = b;
	}
	
	public void updateLighting()
	{
		line.setEnd(new double[]{lightPos[0], lightPos[1], lightPos[2]});
	}
	
	public Vector getLightVector(double[] from)
	{
		return new Vector(from[0]-lightPos[0], from[1]-lightPos[1], from[2]-lightPos[2]);
	}
	
	public int Brightness()
	{
		return brightness;
	}

}
