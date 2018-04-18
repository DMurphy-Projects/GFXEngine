package GxEngine3D.Model;

import java.awt.Color;

import GxEngine3D.CalculationHelper.DistanceCalc;
import GxEngine3D.CalculationHelper.ProjectionCalc;
import GxEngine3D.CalculationHelper.VectorCalc;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Lighting.Light;
import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;

public class Polygon3D {
	Color c;
	private RefPoint3D[] shape;
	boolean draw = true;
	double[] newX, newY;
	Polygon2D screenPoly;

	ViewHandler vHandler;

	BaseShape belongsTo;

	double[] cen;//is now centre of polygon not centre of object
	
	public Polygon3D(RefPoint3D[] shape, Color c,
                     ViewHandler v, BaseShape bTo) {
		vHandler = v;
		this.shape = shape;
		this.c = c;
		belongsTo = bTo;
		createPolygon();

		double avX = 0, avY = 0, avZ = 0;
		for (RefPoint3D p : shape) {
			avX += p.X();
			avY += p.Y();
			avZ += p.Z();
		}

		avX /= shape.length;
		avY /= shape.length;
		avZ /= shape.length;
		cen = new double[]{avX, avY, avZ};
	}

	void createPolygon() {
		screenPoly = new Polygon2D(new double[getShape().length],
				new double[getShape().length], c, vHandler,
				belongsTo);
	}

	public void updatePolygon(Camera c, Light l) {
		if (screenPoly == null)
			createPolygon();
		RefPoint3D[] shp = getShape();
		newX = new double[shp.length];
		newY = new double[shp.length];
		draw = true;
		for (int i = 0; i < shp.length; i++) {
			Projection focus = ProjectionCalc.calculateFocus(c.From(),
					shp[i].X(), shp[i].Y(), shp[i].Z(),
					c.W1, c.W2, c.P);
			if (focus.TValue() < 0) {
				draw = false;
				break;
			}
			newX[i] = vHandler.CenterX() + (focus.Point()[0]*vHandler.Zoom()) - (c.Focus().Point()[0]*vHandler.Zoom());
			newY[i] = vHandler.CenterY() + (focus.Point()[1]*vHandler.Zoom()) - (c.Focus().Point()[1]*vHandler.Zoom());
		}

		screenPoly.draw = draw;
		if (draw) {
			Plane lPlane = new Plane(this);
			lPlane.setP(cen);
			screenPoly.lighting = belongsTo.getLighting().doLighting(l, lPlane, c);
			screenPoly.updatePolygon(newX, newY);
		}
	}

	public double getDist(double[] from) {
		double total = 0;
		for (int i = 0; i < getShape().length; i++) {
			// System.out.println(GetDistanceToP(i));
			total += DistanceCalc.getDistance(from, getShape()[i].toArray());
		}
		return total / getShape().length;
	}

	public Polygon3D[] splitAlong(SplittingPackage[] pack)
	{
		Polygon3D[] pArr = new Polygon3D[2];
		int start = pack[0].index;
		int end  = pack[1].index;
		int cur = start;

		int size = end-start;
		RefPoint3D[] shape = new RefPoint3D[size+2];//for the two split points
		//add start of split
		shape[0] = new RefPoint3D(pack[0].getPoint());
		for (int i=0;i<size;i++)
		{
			shape[i+1] = this.shape[cur];
			cur++;
			if (cur >= this.shape.length)
			{
				cur = 0;
			}
		}
		//add end of split, technically size+2 -1
		shape[size+1] = new RefPoint3D(pack[1].getPoint());
		pArr[0] = new Polygon3D(shape, c, vHandler, belongsTo);
		size = start+(this.shape.length-end);
		shape = new RefPoint3D[size+2];
		cur = end;
		//i = (start - 0) + (length-end)
		//add start of split
		shape[0] = new RefPoint3D(pack[1].getPoint());
		for(int i=0;i<size;i++)
		{
			shape[i+1] = this.shape[cur];
			cur++;
			if (cur >= this.shape.length)
			{
				cur = 0;
			}
		}
		//add end of split
		shape[size+1] = new RefPoint3D(pack[0].getPoint());
		pArr[1] = new Polygon3D(shape, c, vHandler, belongsTo);
		return pArr;
	}

	@Override
	public String toString() {
		String s = "";
		for (RefPoint3D dp : getShape()) {
			s += dp.toString() + " ";
		}
		return s;
	}

	public RefPoint3D[] getShape() {
		return shape;
	}

	public Polygon2D get2DPoly()
	{
		if (screenPoly == null)
			createPolygon();
		return screenPoly;
	}

	public BaseShape getBelongsTo() {
		return belongsTo;
	}

	public boolean canDraw()
	{
		return draw;
	}
}
