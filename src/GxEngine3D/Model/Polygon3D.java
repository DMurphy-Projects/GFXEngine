package GxEngine3D.Model;

import java.awt.Color;

import GxEngine3D.CalculationHelper.DistanceCalc;
import GxEngine3D.CalculationHelper.ProjectionCalc;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Lighting.Light;
import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;

//TODO merge this class with polygon, it servers no purpose anymore
public class Polygon3D {
	Color c;
	private RefPoint3D[] shape;
	boolean draw = true;
	double[] newX, newY;
	Polygon2D screenPoly;

	ViewHandler vHandler;

	BaseShape belongsTo;

	double[] cen;
	
	public Polygon3D(RefPoint3D[] shape, Color c,
                     ViewHandler v, BaseShape bTo) {
		vHandler = v;
		this.setShape(shape);
		this.c = c;
		belongsTo = bTo;
		createPolygon();

		cen = belongsTo.getRefPoint();
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
			screenPoly.lighting = belongsTo.getLighting().doLighting(l, new Plane(this), c);
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

	public void setShape(RefPoint3D[] shape) {
		this.shape = shape;
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
