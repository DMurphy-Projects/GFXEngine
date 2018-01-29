package GxEngine3D.Model;

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
		String s = "";
		for (double d:point)
			s+=d+" ";
		s+= tValue;
		return s;
	}
}
