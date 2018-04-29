package Shapes.Shape2D;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Lighting.ILightingStrategy;
import GxEngine3D.Lighting.Light;
import GxEngine3D.Model.Plane;
import GxEngine3D.Model.RefPoint3D;
import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;

public class Line extends BaseShape {
	
	public Line(double sx, double sy, double sz, double ex, double ey, double ez) {
		super(sx, sy, sz, ex-sx, ey-sy, ez-sz, Color.black);

		lighting = new ILightingStrategy() {
			@Override
			public double doLighting(Light l, Plane p, Camera c) {
				return 1;
			}
		};
	}

	@Override
	protected void createShape() {
		points.add(new RefPoint3D(x, y, z));
		points.add(new RefPoint3D(x+width, y+length, z+height));

		addEdge(new RefPoint3D[]{points.get(0), points.get(1)});
		addPoly(new RefPoint3D[]{points.get(0), points.get(1)}, c);
	}

	public void setStart(double[] pos) {
		RefPoint3D p = points.get(points.size()-2);
		p.setX(pos[0]);
		p.setY(pos[1]);
		p.setZ(pos[2]);
		scheduleUpdate();
	}

	public void setEnd(double[] pos) {
		RefPoint3D p = points.get(points.size()-1);
		p.setX(pos[0]);
		p.setY(pos[1]);
		p.setZ(pos[2]);
		scheduleUpdate();
	}

	@Override
	public void draw(Graphics g, Polygon p) {
		for (int i=0;i<p.npoints-1;i++)
		{
			g.drawLine(p.xpoints[i], p.ypoints[i], p.xpoints[i+1], p.ypoints[i+1]);
		}
	}

	@Override
	public void split(double maxSize) {
		//does not need to be split
	}
}
