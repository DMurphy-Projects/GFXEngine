package GxEngine3D.Model;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;

import GxEngine3D.Controller.GXController;
import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;

public class Polygon2D {
	Polygon P;
	Color c;
	boolean draw = true;
	double lighting = 1;

	private ViewHandler vHandler;

	BaseShape belongsTo;

	private boolean hover = false;

	String debug = "";

	@Override
	public String toString() {
		return debug;
	}

	public Polygon2D(double[] x, double[] y, Color c,
					 ViewHandler v, BaseShape bTo) {
		belongsTo = bTo;
		vHandler = v;
		P = new Polygon();
		for (int i = 0; i < x.length; i++) {
			P.addPoint((int) x[i], (int) y[i]);
			debug += "{"+x[i]+" "+y[i]+"}";
		}
		this.c = c;
	}

	public void hover()
	{
		hover = true;
	}

	public Polygon getPolygon()
	{
		return P;
	}

	public void updatePolygon(double[] x, double[] y) {
		P.reset();
		for (int i = 0; i < x.length; i++) {
			P.xpoints[i] = (int) x[i];
			P.ypoints[i] = (int) y[i];
			P.npoints = x.length;
		}
	}

	public void drawPolygon(Graphics g) {
		if (draw) {
			//to edit brightness directly convert to hsb
			float[] temp = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), new float[3]);
			g.setColor(new Color(Color.HSBtoRGB(temp[0], temp[1], (float) lighting)));
			belongsTo.draw(g, P);
			if (hover && GXController.canHover()) {
				g.setColor(new Color(255, 255, 255, 100));
				belongsTo.drawHighlight(g, P);
			}
			if (GXController.hasOutlines()) {
				g.setColor(new Color(0, 0, 0));
				belongsTo.drawOutlines(g, P);
			}
			hover = false;
		}
	}

	public boolean MouseOver() {
		return P.contains(vHandler.CenterX(), vHandler.CenterY());
	}
}
