package GxEngine3D.Model;

import java.util.Arrays;

public class Projection {
	
	private double[] point;
	private double tValue;//denotes which side the point is on with respect to plane, -1 behind, 1 in front
	
	public Projection(double[] p, double t) {
		point = p;
		tValue = t;
	}

	public double TValue() {
		return tValue;
	}

	public double[] Point() {
		return point;
	}

	@Override
	public String toString() {
		return String.format("Point: %s, TValue: %s", Arrays.toString(point), tValue);
	}
}
