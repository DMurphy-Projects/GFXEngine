package GxEngine3D.Model;

import GxEngine3D.CalculationHelper.VectorCalc;

public class Plane {
	Vector v1, v2;
	private Vector normalVector;// vector that is perpendiular to surface
	private double[] point = new double[3];// point on the plane

	public Plane(Polygon3D dp)
	{
		RefPoint3D[] p = dp.getShape();
		point[0] = p[0].X();
		point[1] = p[0].Y();
		point[2] = p[0].Z();

		//object doesnt have enough point for cross vector
		//probably a line or single point
		if(p.length > 2) {
			v1 = new Vector(p[1].X() - p[0].X(), p[1].Y() - p[0].Y(), p[1].Z()
					- p[0].Z());
			v2 = new Vector(p[2].X() - p[0].X(), p[2].Y() - p[0].Y(), p[2].Z()
					- p[0].Z());
			normalVector = new Vector(VectorCalc.norm(VectorCalc.cross(v1.toArray(), v2.toArray())));
		}
	}

	public Plane(Vector VE1, Vector VE2, double[] Z) {
		setP(Z);

		v1 = VE1;

		v2 = VE2;

		normalVector = new Vector(VectorCalc.norm(VectorCalc.cross(v1.toArray(), v2.toArray())));
	}

	public Vector getV1()
	{
		return v1;
	}
	public Vector getV2()
	{
		return v2;
	}

	public boolean isTowards(double[] point)
	{
		return VectorCalc.dot(VectorCalc.sub(this.point, point), getNV().toArray()) > 0;
	}


	public Vector getNV() {
		return normalVector;
	}
	public Vector getNV(double[] point)
	{
		if (isTowards(point)) {
			return getNV();
		}
		else
		{
			return getFlipNV();
		}
	}


	public Vector getFlipNV()
	{
		return v2.crossProduct(v1);
	}

	public void flipNV()
	{
		Vector temp = v1;
		v1 = v2;
		v2 = temp;
		setNV(v1.crossProduct(v2));
	}


	public void setNV(Vector nV) {
		normalVector = nV;
	}

	public double[] getP() {
		return point;
	}

	public void setP(double[] p) {
		point = p;
	}
}
