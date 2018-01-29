package GxEngine3D.CalculationHelper;

import GxEngine3D.Projection;

public class VectorCalc {

	public static double[] bounce_on_plane(double[] planeNorm, double[] vector)
	{
		double dot = dot_v3v3(vector, planeNorm);
		dot *= -2;
		double[] bounce = mul_v3_fl(planeNorm, dot);
		bounce = add_v3v3(bounce, vector);
		//System.out.println(bounce[0]+" "+bounce[1]+" "+bounce[2]);

		return bounce;
	}

	public static Projection isect_line_plane(double[] p0, double[] p1,
											  double[] p_co, double[] p_no) {
		double[] u = sub_v3v3(p1, p0);
		return isect_vec_plane(p0, u, p_co, p_no);
	}
	public static Projection isect_vec_plane(double[] from,
														 double[] viewToPoint, double[] pp, double[] pnv) {
		double dot = dot_v3v3(pnv, from);
		double dot3 = dot_v3v3(pnv, pp);
		double f = dot3 - dot;
		return new Projection(add_v3v3(mul_v3_fl(viewToPoint, f), from), f);
	}


	public static Projection isect_line_plane_perspective(double[] p0, double[] p1,
														  double[] p_co, double[] p_no) {
		double[] u = sub_v3v3(p1, p0);
		return isect_vec_plane_perspective(p0, u, p_co, p_no);
	}
	public static Projection isect_vec_plane_perspective(double[] from,
														 double[] viewToPoint, double[] pp, double[] pnv) {
		double dot = dot_v3v3(pnv, from);
		double dot2 = dot_v3v3(pnv, viewToPoint);
		double dot3 = dot_v3v3(pnv, pp);
		double f = (dot3 - dot)/dot2;//dot2 not needed for iset plane as its for perspective
		return new Projection(add_v3v3(mul_v3_fl(viewToPoint, f), from), f);
	}

	public static double[] add_v3v3(double[] v0, double[] v1) {
		return new double[] { v0[0] + v1[0], v0[1] + v1[1], v0[2] + v1[2], };
	}

	public static double[] sub_v3v3(double[] v0, double[] v1) {
		return new double[] { v0[0] - v1[0], v0[1] - v1[1], v0[2] - v1[2], };
	}

	public static double dot_v3v3(double[] v0, double[] v1) {
		return (v0[0] * v1[0]) + (v0[1] * v1[1]) + (v0[2] * v1[2]);
	}

	public static double len_squared_v3(double[] v0) {
		return dot_v3v3(v0, v0);
	}

	public static double[] mul_v3_fl(double[] v0, double f) {
		return new double[] { v0[0] * f, v0[1] * f, v0[2] * f, };
	}

	public static double[] div_v3_fl(double[] v0, double f) {
		return new double[] { v0[0] / f, v0[1] / f, v0[2] / f, };
	}

	public static double len_v3_fl(double[] v0) {
		return (double) Math.sqrt(len_squared_v3(v0));
	}

	public static double[] cross(double[] v0, double[] v1) {
		return new double[] { v0[1] * v1[2] - v0[2] * v1[1],
				v0[2] * v1[0] - v0[0] * v1[2], v0[0] * v1[1] - v0[1] * v1[0] };
	}

	public static double[] mul_arr(double[] v0, double[] v1) {
		return new double[] { (v0[0] * v1[0]), (v0[1] * v1[1]), (v0[2] * v1[2]) };
	}

}
