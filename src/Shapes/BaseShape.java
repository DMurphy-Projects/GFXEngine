package Shapes;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.util.ArrayList;

import DebugTools.TextOutput;
import GxEngine3D.Helper.MatrixHelper;
import GxEngine3D.Helper.VectorCalc;
import GxEngine3D.Lighting.AltLighting;
import GxEngine3D.Lighting.ILightingStrategy;
import GxEngine3D.Model.Matrix.Matrix;
import GxEngine3D.Model.Polygon3D;
import GxEngine3D.Model.RefPoint3D;
import Shapes.Split.ISplitStrategy;
import Shapes.Split.SplitIntoTriangles;

//NOTE:
//--when creating a shape, add points using addPoint()
//--when creating edges or polygons, be sure to reference "points" NOT "relativePoints"
//--shapes relative points should be centred around {0.5, 0.5, 0.5}
//--as of now, relative points do NOT need to be normalised, but should start at {0, 0, 0}
//--translation is now relative to orientation of the object
public abstract class BaseShape implements IShape, IDrawable, IManipulable {

	//information about the shapes current transformations
	protected double pitch = 0, yaw = 0, roll = 0, x = 0, y = 0, z = 0, sX = 1, sY = 1, sZ = 1;
	Matrix scale, rotation, translation, combined;

	protected Color c;

	static int id = 0;
	int curId;
	private boolean needsUpdate = true, init = false;

	//stores the points relative to each other
	protected ArrayList<double[]> relativePoints = new ArrayList<double[]>();
	//stores the points after matrix transforms
	protected ArrayList<RefPoint3D> points = new ArrayList<RefPoint3D>();

	protected ArrayList<RefPoint3D[]> edges = new ArrayList<RefPoint3D[]>();
	protected ArrayList<Polygon3D> polys = new ArrayList<Polygon3D>();
	
	protected static String name = "Shape";
	protected ILightingStrategy lighting;

	double[] anchor;

	protected ISplitStrategy triangles = new SplitIntoTriangles();

	public BaseShape(Color c) {
		this.c = c;

		double[][] identity = MatrixHelper.setupIdentityMatrix();

		scale = new Matrix(4, 4);
		scale.insertMatrix(identity);
		translation = new Matrix(4, 4);
		translation.insertMatrix(identity);
		anchor = new double[3];;

		rotation = new Matrix(MatrixHelper.setupPitchRotation(0));

		curId = BaseShape.id++;
		lighting = new AltLighting();
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

	protected void addPoint(double[] p)
	{
		relativePoints.add(p);
		points.add(new RefPoint3D(p));
	}
	protected void addEdge(RefPoint3D[] edge)
	{
		edges.add(edge);
	}
	protected void addPoly(RefPoint3D[] poly, Color c)
	{
		polys.add(new Polygon3D(poly, c, this));
	}

	protected void scheduleUpdate()
	{
		//set global update flag
		if (!needsUpdate) {
			needsUpdate = true;
		}
	}

	public void init()
	{
		if (!init)
		{
			init = true;
			createShape();
		}
		else
		{
			TextOutput.println(this.toString() + " is already initialised");
		}
	}

	public void translate(double x, double y, double z)
	{
		if (x != 0 || y != 0 || z != 0)
		{
			this.x += x; this.y += y; this.z += z;
			translation = new Matrix(MatrixHelper.setupTranslateMatrix(this.x, this.y, this.z));
			scheduleUpdate();
		}
	}

	@Override
	public void absoluteTranslate(double x, double y, double z) {
		//if something is different
		if (this.x != x || this.y != y || this.z != z) {
			this.x = x; this.y = y; this.z = z;
			translation = new Matrix(MatrixHelper.setupTranslateMatrix(x, y, z));
			scheduleUpdate();
		}
	}

	public void scale(double x, double y, double z)
	{
		//the check is about as expensive as remaking the matrix
		this.sX += x; this.sY += y; this.sZ += z;
		double[][] scale = new double[4][4];
		scale[0][0] = x;
		scale[1][1] = y;
		scale[2][2] = z;
		scale[3][3] = 1;
		this.scale = new Matrix(this.scale.scaleMatrix(scale));
		scheduleUpdate();
	}

	@Override
	public void absoluteScale(double x, double y, double z) {
		//if they are all the same, there is nothing to do
		if (this.sX != x || this.sY != y || this.sZ != z) {
			this.sX = x; this.sY = y; this.sZ = z;
			double[][] scale = new double[4][4];
			scale[0][0] = x;
			scale[1][1] = y;
			scale[2][2] = z;
			scale[3][3] = 1;
			this.scale = new Matrix(scale);
			scheduleUpdate();
		}
	}

	@Override
	public void rotate(double pitch, double yaw, double roll) {
		double[][] d = null;

		if (pitch != 0) {
			this.pitch += pitch;
			d = MatrixHelper.setupPitchRotation(pitch);
			rotation = new Matrix(rotation.matrixMultiply(d));
		}

		if (yaw != 0) {
			this.yaw += yaw;
			d = MatrixHelper.setupYawRotation(yaw);
			rotation = new Matrix(rotation.matrixMultiply(d));
		}

		if (roll != 0) {
			this.roll += roll;
			d = MatrixHelper.setupRollRotation(roll);
			rotation = new Matrix(rotation.matrixMultiply(d));
		}

		if (d != null)
		{
			scheduleUpdate();
		}
	}

	@Override
	public void absoluteRotate(double pitch, double yaw, double roll) {
		double[][] d = null;
		Matrix m = null;

		if (pitch != this.pitch) {
			this.pitch = pitch;
			d = MatrixHelper.setupPitchRotation(pitch);
			m = new Matrix(d);
		}

		if (yaw != this.yaw) {
			this.yaw = yaw;
			d = MatrixHelper.setupYawRotation(yaw);
			if (m == null)
			{
				m = new Matrix(d);
			}
			else {
				m = new Matrix(m.matrixMultiply(d));
			}
		}

		if (roll != this.roll) {
			this.roll = roll;
			d = MatrixHelper.setupRollRotation(roll);
			if (m == null)
			{
				m = new Matrix(d);
			}
			else {
				m = new Matrix(m.matrixMultiply(d));
			}
		}
		//check if any values were altered
		if (d != null)
		{
			rotation = m;
			scheduleUpdate();
		}
	}

	@Override
	public void setAnchor(double[] d)
	{
		anchor = d;
	}

	protected abstract void createShape();

	public double[] findCentre() {
		double[] centre = new double[]{0.5, 0.5, 0.5};
		update();
		return transform(centre);
	}


	private double[] transform(int i)
	{
		double[] point = relativePoints.get(i);
		return transform(point);
	}

	private double[] transform(double[] point)
	{
		point = combined.pointMultiply(VectorCalc.sub(point, anchor));
		point = new double[]{
				point[0] / point[3],
				point[1] / point[3],
				point[2] / point[3],
		};
		return point;
	}

	// gives back rotated relativePoints x, y, z :0, 1, 2
	public void update() {
		if (needsUpdate) {
			needsUpdate = false;

			//uses reverse order since we don't want the following to happen:
			//-scale affecting translation
			//-rotation affecting translation
			combined = translation;
			combined = new Matrix(combined.matrixMultiply(rotation));
			combined = new Matrix(combined.matrixMultiply(scale));

			for (int i=0;i<relativePoints.size();i++)
			{
				points.get(i).setArray(transform(i));
			}
		}
	}

	public void split(double maxSize)
	{
//		triangles.split(maxSize, polys, c, this);
		//subDivide.split(maxSize, polys, c, v, this);
		//middleSplit.split(maxSize, polys, c, v, this);
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
