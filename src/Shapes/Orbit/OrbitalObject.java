package Shapes.Orbit;

import java.awt.Color;
import java.util.ArrayList;

import GxEngine3D.RefPoint3D;
import GxEngine3D.CalculationHelper.RotationCalc;
import GxEngine3D.ViewHandler;
import Shapes.BaseShape;
import Shapes.Cube;
import Shapes.IShape;

public class OrbitalObject extends BaseShape {

	// rotation and actual rotation values
	private double xR, yR, zR;

	// temp coords for simultaneous updating
	private double xT, yT, zT;

	ArrayList<OrbitalObject> children = new ArrayList<OrbitalObject>();

	OrbitalObject parent;

	public OrbitalObject(double x, double y, double z, double xR, double yR,
			double zR, Color c, ViewHandler v) {
		super(x, y, z, .1, .1, .1, c, v);
		this.x = x;
		this.y = y;
		this.z = z;
		this.xR = xR;
		this.yR = yR;
		this.zR = zR;
	}

	public void setParent(OrbitalObject o) {
		parent = o;
	}

	public void add(OrbitalObject o) {
		children.add(o);
		o.setParent(this);
	}

	private void move(double x, double y, double z) {
		this.xT += x;
		this.yT += y;
		this.zT += z;
		for (OrbitalObject o:children)
			o.move(x, y, z);
	}

	private void updateValues() {
		x += xT;
		y += yT;
		z += zT;
		for (RefPoint3D p : points) {
			p.setX(p.X() + xT);
			p.setY(p.Y() + yT);
			p.setZ(p.Z() + zT);
		}
		xT = 0;
		yT = 0;
		zT = 0;
		/*xA += xR;
		yA += yR;
		zA += zR;
		*/
	}

	public void update() {
		// work out orbit
		// new position - only want ho much it moves by
		if (parent == null) {
			parent = this;
		}
		double[] nPoints = RotationCalc.rotateFull(x, y, z, parent.x, parent.y,
				parent.z, xR, yR, zR);
		nPoints[0] = nPoints[0] - x;
		nPoints[1] = nPoints[1] - y;
		nPoints[2] = nPoints[2] - z;
		// cascade to children
		move(nPoints[0], nPoints[1], nPoints[2]);
		// update children
		for (OrbitalObject o : children)
			o.update();
		// update values
		updateValues();
	}

	@Override
	protected void createShape() {
		IShape cube = new Cube(x, y, z, width, height, length, c, v);
		points = cube.getPoints();
		polys = cube.getShape();
	}
}
