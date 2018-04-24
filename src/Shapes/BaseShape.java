package Shapes;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.util.ArrayList;

import GxEngine3D.CalculationHelper.DistanceCalc;
import GxEngine3D.CalculationHelper.RotationCalc;
import GxEngine3D.Lighting.ILightingStrategy;
import GxEngine3D.Lighting.StandardLighting;
import GxEngine3D.Model.Polygon3D;
import GxEngine3D.Model.RefPoint3D;
import GxEngine3D.View.ViewHandler;
import Shapes.Split.ISplitStrategy;
import Shapes.Split.MidPointSplit;
import Shapes.Split.SplitIntoTriangles;
import Shapes.Split.SubDivideOnPoint;

public abstract class BaseShape implements IShape, IDrawable, IManipulable {

	protected double x, y, z, width, length, height;
	double mToX, mToY, mToZ;
	protected double rotXY = 0, rotXZ = 0, rotYZ = 0;
	protected double tXY = 0, tXZ = 0, tYZ = 0;
	protected Color c;

	static int id = 0;
	int curId;

	protected ArrayList<RefPoint3D> points = new ArrayList<RefPoint3D>();
	protected ArrayList<RefPoint3D[]> edges = new ArrayList<RefPoint3D[]>();
	protected ArrayList<Polygon3D> polys = new ArrayList<Polygon3D>();

	protected ViewHandler v;

	private Polygon3D hover;
	
	protected static String name = "Shape";
	protected ILightingStrategy lighting;

	protected ISplitStrategy triangles = new SplitIntoTriangles();

	public BaseShape(double x, double y, double z, double width, double length,
			double height, Color c, ViewHandler v) {
		this.c = c;
		this.x = x;
		this.y = y;
		this.z = z;
		moveTo(x, y, z);
		this.width = width;
		this.length = length;
		this.height = height;
		this.v = v;
		curId = BaseShape.id++;
		lighting = new StandardLighting();
		createShape();
	}

	public ArrayList<Polygon3D> getShape() {
		return polys;
	}

	public ArrayList<RefPoint3D[]> getEdges() {
		return edges;
	}
	public ArrayList<RefPoint3D> getPoints() {
		return points;
	}

	protected void addEdge(RefPoint3D[] edge)
	{
		edges.add(edge);
	}
	protected void addPoly(RefPoint3D[] poly, Color c)
	{
		polys.add(new Polygon3D(poly, c, v, this));
	}

	protected void scheduleUpdate()
	{
		if (!mNeedUpdate)
		{
			mNeedUpdate = true;
		}
	}

	public void yaw(double angle) {
		tXY += angle;
		scheduleUpdate();
	}

	public void pitch(double angle) {
		tXZ += angle;
		scheduleUpdate();
	}

	public void roll(double angle) {
		tYZ += angle;
		scheduleUpdate();
	}

	public void moveTo(double x, double y, double z)
	{
		mToX = x;
		mToY = y;
		mToZ = z;
		scheduleUpdate();
	}

	protected abstract void createShape();

	public double[] findCentre() {
		double avX = 0, avY = 0, avZ = 0;
		for (RefPoint3D p : points) {
			avX += p.X();
			avY += p.Y();
			avZ += p.Z();
		}

		avX /= points.size();
		avY /= points.size();
		avZ /= points.size();
		return new double[] { avX, avY, avZ };
	}

	public double getDistanceFrom(double[] point) {
		return DistanceCalc.getDistance(point, findCentre());
	}

	// gives back rotated points x, y, z :0, 1, 2

	private boolean mNeedUpdate = true;

	public void update() {
		if (mNeedUpdate) {
			mNeedUpdate = false;
			// middle of object
			double[] origin = findCentre();
			double[] p;

			for (int i = 0; i < points.size(); i++) {
				p = RotationCalc.rotateFull(points.get(i).X(), points.get(i).Y(),
						points.get(i).Z(), origin[0], origin[1], origin[2], rotXY
								- tXY, rotXZ - tXZ, rotYZ - tYZ);
				points.get(i).setX((mToX - x) + p[0]);
				points.get(i).setY((mToY - y) + p[1]);
				points.get(i).setZ((mToZ - z) + p[2]);
			}
			x = mToX;
			y = mToY;
			z = mToZ;
			rotXY += tXY - rotXY;
			rotXZ += tXZ - rotXZ;
			rotYZ += tYZ - rotYZ;
		}
	}

	public void split(double maxSize)
	{
		triangles.split(maxSize, polys, c, v, this);
		//subDivide.split(maxSize, polys, c, v, this);
		//middleSplit.split(maxSize, polys, c, v, this);
	}

	public void hover(Polygon3D o) {
		hover = o;
		hover.get2DPoly().hover();
	}

	public double[] getRefPoint()
	{
		return findCentre();
	}

	@Override
	public void draw(Graphics g, Polygon p) {
		g.fillPolygon(p);
	}
	public void drawOutlines(Graphics g, Polygon p){g.drawPolygon(p);}
	public void drawHighlight(Graphics g, Polygon p){g.fillPolygon(p);}
	public static String getName()
	{
		return name;
	}
	public ILightingStrategy getLighting() {return lighting;}
}
