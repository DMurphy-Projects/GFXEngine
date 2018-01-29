package Shapes.Rubix;

import java.util.ArrayList;

import GxEngine3D.RefPoint3D;

public class RubixCubelet {

	private ArrayList<RefPoint3D> cube;
	
	public RubixCubelet(ArrayList<RefPoint3D> cube)
	{
		this.cube = cube;
	}
	public ArrayList<RefPoint3D> Cube()
	{
		return cube;
	}
	public boolean is(RefPoint3D p[])
	{
		for (RefPoint3D p1:p)
		{
			if (!is(p1))
			{
				return false;
			}
		}
		return true;
	}
	public boolean is(RefPoint3D p)
	{
		for (RefPoint3D p1:cube)
		{
			if (p == p1)
			{
				return true;
			}
		}
		return false;
	}
	public RefPoint3D Centre()
	{
		double avX = 0, avY = 0, avZ = 0;
		for (RefPoint3D p : cube) {
			avX += p.X();
			avY += p.Y();
			avZ += p.Z();
		}

		avX /= cube.size();
		avY /= cube.size();
		avZ /= cube.size();
		return new RefPoint3D(avX, avY, avZ);
	}

	
}
