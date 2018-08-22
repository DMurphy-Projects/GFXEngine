package GxEngine3D.Model;

import java.math.BigDecimal;

public class RefPoint3D {

	private double x, y, z;
	
	public RefPoint3D(double x, double y, double z)
	{
		setX(x);
		setY(y);
		setZ(z);
	}
	public RefPoint3D(double[] arr)
	{
		if (arr.length == 3)
		{
			setX(arr[0]);
			setY(arr[1]);
			setZ(arr[2]);
		}
	}
	public double X()
	{
		return x;
	}
	public double Y()
	{
		return y;
	}
	
	public double Z()
	{
		return z;
	}
	
	public void setX(double x)
	{
		this.x = x;
	}
	public void setY(double y)
	{
		this.y = y;
	}
	public void setZ(double z)
	{
		this.z = z;
	}
	public void setArray(double[] arr)
	{
		setX(arr[0]);
		setY(arr[1]);
		setZ(arr[2]);
	}
	
	public double[] toArray()
	{
		return new double[]{x, y, z};
	}
	
	@Override
	public String toString() {
		return "{"+x+", "+y+", "+z+"}";
	}

	public static double round(double unrounded, int precision)
	{
		BigDecimal bd = new BigDecimal(unrounded);
		BigDecimal rounded = bd.setScale(precision, BigDecimal.ROUND_FLOOR);
		return rounded.doubleValue();
	}
}
