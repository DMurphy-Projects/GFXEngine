package GxEngine3D.Helper;

import GxEngine3D.Model.Plane;
import GxEngine3D.Model.Projection;
import GxEngine3D.Model.Vector;

public class ProjectionCalc {

	public static Projection calculateFocus(double[] ViewFrom,
                                     double x, double y, double z, Vector w1, Vector w2, Plane cPlane) {
		Projection projP = getProj(ViewFrom, x, y, z, cPlane);
		double[] drawP = getDrawP(projP.Point()[0], projP.Point()[1],
				projP.Point()[2], w1, w2);
		return new Projection(drawP, projP.TValue());
	}

	public static Projection getProj(double[] ViewFrom, double x,
			double y, double z, Plane P) {

		return isect_line_plane_perspective(ViewFrom, new double[]{x, y, z},
				P.getP(), P.getNV().toArray());
	}

	static double[] getDrawP(double x, double y, double z, Vector w1, Vector w2) {
		double DrawX = w2.X() * x + w2.Y() * y + w2.Z() * z;
		double DrawY = w1.X() * x + w1.Y() * y + w1.Z() * z;
		return new double[] { DrawX, DrawY };
	}

	public static Vector getRotationVector(double[] ViewFrom, double[] ViewTo) {
		double dx = Math.abs(ViewFrom[0] - ViewTo[0]);
		double dy = Math.abs(ViewFrom[1] - ViewTo[1]);
		double xRot, yRot;
		xRot = dy / (dx + dy);
		yRot = dx / (dx + dy);

		if (ViewFrom[1] > ViewTo[1])
			xRot = -xRot;
		if (ViewFrom[0] < ViewTo[0])
			yRot = -yRot;

		Vector V = new Vector(xRot, yRot, 0);
		return V;
	}

	public static double[] bounce_on_plane(double[] planeNorm, double[] vector)
	{
		double dot = VectorCalc.dot(vector, planeNorm);
		dot *= -2;
		double[] bounce = VectorCalc.mul_v_d(planeNorm, dot);
		bounce = VectorCalc.add(bounce, vector);
		return bounce;
	}

	public static Projection isect_line_plane(double[] p0, double[] p1,
											  double[] p_co, double[] p_no) {
		double[] u = VectorCalc.sub(p1, p0);
		return isect_vec_plane(p0, u, p_co, p_no);
	}
	public static Projection isect_vec_plane(double[] from,
											 double[] vec, double[] pp, double[] pnv) {
		double dot = VectorCalc.dot(pnv, from);
		double dot3 = VectorCalc.dot(pnv, pp);
		double f = dot3 - dot;
		return new Projection(VectorCalc.add(VectorCalc.mul_v_d(vec, f), from), f);
	}


	public static Projection isect_line_plane_perspective(double[] p0, double[] p1,
														  double[] p_co, double[] p_no) {
		double[] u = VectorCalc.sub(p1, p0);
		return isect_vec_plane_perspective(p0, u, p_co, p_no);
	}
	public static Projection isect_vec_plane_perspective(double[] from,
														 double[] viewToPoint, double[] pp, double[] pnv) {
		double dot = VectorCalc.dot(pnv, from);
		double dot2 = VectorCalc.dot(pnv, viewToPoint);
		double dot3 = VectorCalc.dot(pnv, pp);
		double f = (dot3 - dot)/dot2;//dot2 not needed for iset plane as its for perspective
		return new Projection(VectorCalc.add(VectorCalc.mul_v_d(viewToPoint, f), from), f);
	}
}
