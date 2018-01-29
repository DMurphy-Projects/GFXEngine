package GxEngine3D.Model;

public class Vector {
	double x, y, z, len;

	public Vector(double[] v) {
		norm(v[0], v[1], v[2], true);
	}

	public Vector(double x, double y, double z) {
		norm(x, y, z, true);
	}

	public Vector(double x, double y, double z, boolean n) {
		norm(x, y, z, n);
	}
	public Vector(double[] v, boolean n) {
		norm(v[0], v[1], v[2], n);
	}
	
	private void norm(double x, double y, double z, boolean normalise) {
		len = Math.sqrt((x * x) + (y * y) + (z * z));

		this.x = x;
		this.y = y;
		this.z = z;
		//for if 0
		if (len > 0 && normalise) {
			this.x = x / len;
			this.y = y / len;
			this.z = z / len;
		}
	}
	
	public Vector scale(double d) {
		return new Vector(x * d, y * d, z * d);
	}
	
	public Vector multiply(Vector v)
	{
		return multiply(v,true);
	}
	public Vector multiply(Vector v, boolean norm)
	{
		return new Vector(x*v.X(), y*v.Y(), z*v.Z(), norm);
	}

	public double dot(Vector v) {
		return (v.X() * x) + (v.Y() * y) + (v.Z() * z);
	}
	public Vector crossProduct(Vector V) {

		return crossProduct(V, true);
	}
	public Vector crossProduct(Vector V, boolean norm)
	{
		Vector CrossVector = new Vector((y * V.z) - (z * V.y), (z * V.x) - (x * V.z), (x
				* V.y) - (y * V.x), norm);
		return CrossVector;
	}

	public Vector add(Vector v) {
		return new Vector(x + v.X(), y + v.Y(), z + v.Z());
	}

	public Vector sub(Vector v) {
		return new Vector(x - v.X(), y - v.Y(), z - v.Z());
	}

	public double X() {
		return x;
	}

	public double Y() {
		return y;
	}

	public double Z() {
		return z;
	}
	
	public double Length()
	{
		return len;
	}

	public double[] toArray() {
		return new double[] { x, y, z };
	}

	@Override
	public String toString() {
		return "{" + x + ", " + y + ", " + z + "}";
	}
}
