package GxEngine3D.CalculationHelper;

public class DistanceCalc {

	/*
	public static double getDistance(double[] d1, double[] d2) {
		double xDist = d1[0] - d2[0];
		xDist *= xDist;
		double yDist = d1[1] - d2[1];
		yDist *= yDist;
		double zDist = d1[2] - d2[2];
		zDist *= zDist;
		return Math.sqrt(xDist + yDist + zDist);
	}

*/
	public static double getDistance(double[] d1, double[] d2) {
		double total = 0;
		// assumes same dimensions
		for (int i = 0; i < d1.length; i++) {
			total += (d1[i] - d2[i]) * (d1[i] - d2[i]);
		}
		return Math.sqrt(total);
	}

}
