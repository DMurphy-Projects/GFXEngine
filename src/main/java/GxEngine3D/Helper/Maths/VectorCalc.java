package GxEngine3D.Helper.Maths;

public class VectorCalc {

	private static double epsilon = 1e-10;;

	public static double[] add(double[] v0, double[] v1) {
		double[] dArr = new double[v0.length];
		for (int i=0;i<v0.length;i++)
		{
			dArr[i] = v0[i] + v1[i];
		}
		return dArr;
	}

	public static double[] sub(double[] v0, double[] v1) {
		double[] dArr = new double[v0.length];
		for (int i=0;i<v0.length;i++)
		{
			dArr[i] = v0[i] - v1[i];
		}
		return dArr;
	}

	public static double dot(double[] v0, double[] v1) {
		double d = 0;
		for (int i=0;i<v0.length;i++)
		{
			d += v0[i] * v1[i];
		}
		return d;
	}

	public static double len_squared(double[] v0) {
		return dot(v0, v0);
	}

	public static double[] mul_v_d(double[] v0, double d) {
		double[] dArr = new double[v0.length];
		for (int i=0;i<v0.length;i++)
		{
			dArr[i] = v0[i] * d;
		}
		return dArr;
	}

	public static double[] div_v_d(double[] v0, double d) {
		return mul_v_d(v0, 1d/d);
	}

	public static double len(double[] v0) {
		return Math.sqrt(len_squared(v0));
	}

	public static double[] cross(double[] v0, double[] v1) {
		return new double[] { (v0[1] * v1[2]) - (v0[2] * v1[1]),
				(v0[2] * v1[0]) - (v0[0] * v1[2]), (v0[0] * v1[1]) - (v0[1] * v1[0]) };
	}

	public static double[] mul(double[] v0, double[] v1) {
		double[] dArr = new double[v0.length];
		for (int i=0;i<v0.length;i++)
		{
			dArr[i] = v0[i] * v1[i];
		}
		return dArr;
	}

	public static double[] div(double[] v0, double[] v1) {
		double[] dArr = new double[v0.length];
		for (int i=0;i<v0.length;i++)
		{
			dArr[i] = v0[i] / v1[i];
		}
		return dArr;
	}

	public static double[] norm(double[] v0)
	{
		return div_v_d(v0, len(v0));
	}

	public static double[] plane_v3_pointForm(double[] nV, double[] p)
	{
		//in form Ax + Bx + Cx = D
		//nV . (p-p0) gives form
		//so x coefficient is just the nV.x, etc
		double[] pointForm = new double[]{
				nV[0],
				nV[1],
				nV[2],
				0
		};
		//ax + by + cz + d = 0
		//d = -ax -by -cz
		for (int i=0;i<3;i++)
		{
			pointForm[3] -= nV[i] * p[i];
		}
		return pointForm;
	}

	public static boolean v_v_equals(double[] v1, double[] v2)
	{
		v1 = norm(v1);
		v2 = norm(v2);
		for (int i=0;i<v1.length;i++)
		{
			if (Math.abs(v1[i] - v2[i]) > epsilon)
			{
				return false;
			}
		}
		return true;
	}

	public static boolean v_is_null(double[] v)
	{
		for (int i=0;i<v.length;i++)
		{
			if (Math.abs(v[i]) > epsilon)
			{
				return false;
			}
		}
		return true;
	}

	//https://www.lucidarme.me/check-if-a-point-belongs-on-a-line-segment/
	public static boolean p3_in_line_seg(double[] l0, double[] l1, double[] p)
	{
		double[] v0 = VectorCalc.sub(l0, l1);
		double[] v1 = VectorCalc.sub(l0, p);
		double[] cross = VectorCalc.cross(v0, v1);
		if (!v_is_null(cross))
		{
			return false;
		}
		double k_ac = VectorCalc.dot(v0, v1);
		if (k_ac < 0)
		{
			return false;
		}
		else if (k_ac == 0)
		{
			//p shares with l0
			return true;
		}
		double k_ab = VectorCalc.dot(v0, v0);
		if (k_ac > k_ab)
		{
			return false;
		}
		else if (k_ac == k_ab)
		{
			//p shares with l2
			return true;
		}
		return true;
	}

}
