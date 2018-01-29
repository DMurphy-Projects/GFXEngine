package GxEngine3D;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;

import Shapes.BaseShape;
import Shapes.IShape;

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

	void updatePolygon(double[] x, double[] y) {
		P.reset();
		for (int i = 0; i < x.length; i++) {
			P.xpoints[i] = (int) x[i];
			P.ypoints[i] = (int) y[i];
			P.npoints = x.length;
		}
	}

	void drawPolygon(Graphics g) {
		if (draw) {
			g.setColor(new Color((int) (c.getRed() * lighting), (int) (c
					.getGreen() * lighting), (int) (c.getBlue() * lighting)));
			belongsTo.draw(g, P);
			if (hover) {
				g.setColor(new Color(255, 255, 255, 100));
				belongsTo.drawHighlight(g, P);
			}
			if (GXController.outlines) {
				g.setColor(new Color(0, 0, 0));
				belongsTo.drawOutlines(g, P);
			}
			hover = false;
		}
	}

	boolean MouseOver() {
		return P.contains(vHandler.CenterX(), vHandler.CenterY());
	}
}
