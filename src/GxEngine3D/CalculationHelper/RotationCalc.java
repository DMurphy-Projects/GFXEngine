package GxEngine3D.CalculationHelper;

public class RotationCalc {

	public static double[] rotate(double[] p, double[] o, double rotation) {
		double[] newP = new double[2];
		newP[0] = o[0] + (p[0] - o[0]) * Math.cos(rotation) - (p[1] - o[1])
				* Math.sin(rotation);

		newP[1] = o[1] + (p[0] - o[0]) * Math.sin(rotation) + (p[1] - o[1])
				* Math.cos(rotation);
		return newP;
	}
	
	public static double[] rotateFull(double x, double y, double z, double ox,
			double oy, double oz, double yaw, double pitch, double roll) {
		double[] p = rotate(new double[] { x, y }, new double[] { ox, oy }, yaw);
		x = p[0];
		y = p[1];
		p = rotate(new double[] { x, z }, new double[] { ox, oz }, pitch);
		x = p[0];
		z = p[1];
		p = rotate(new double[] { y, z }, new double[] { oy, oz }, roll);
		y = p[0];
		z = p[1];
		return new double[] { x, y, z };
	}
	
}
