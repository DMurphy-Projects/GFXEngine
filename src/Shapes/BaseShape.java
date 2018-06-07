package Shapes;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.util.ArrayList;

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
public abstract class BaseShape implements IShape, IDrawable, IManipulable {

	double pitch = 0, yaw = 0, roll = 0;
	Matrix scale, xRotation, yRotation, zRotation, translation;

	protected Color c;

	static int id = 0;
	int curId;
	private boolean needsUpdate = true;

	//stores the points relative to each other
	protected ArrayList<double[]> relativePoints = new ArrayList<double[]>();
	//stores the points after matrix transforms
	protected ArrayList<RefPoint3D> points = new ArrayList<RefPoint3D>();

	protected ArrayList<RefPoint3D[]> edges = new ArrayList<RefPoint3D[]>();
	protected ArrayList<Polygon3D> polys = new ArrayList<Polygon3D>();
	
	protected static String name = "Shape";
	protected ILightingStrategy lighting;

	protected ISplitStrategy triangles = new SplitIntoTriangles();

	public BaseShape(Color c) {
		this.c = c;

		double[][] identity = new double[4][4];
		identity[0][0] = 1;
		identity[1][1] = 1;
		identity[2][2] = 1;
		identity[3][3] = 1;

		scale = new Matrix(4, 4);
		scale.insertMatrix(identity);
		translation = new Matrix(4, 4);
		translation.insertMatrix(identity);

		curId = BaseShape.id++;
		lighting = new AltLighting();
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
		if (!needsUpdate)
		{
			needsUpdate = true;
		}
	}

	public void translate(double x, double y, double z)
	{
		double[][] translate = new double[4][4];
		translate[0][3] = x;
		translate[1][3] = y;
		translate[2][3] = z;
		this.translation = new Matrix(this.translation.addMatrix(translate));
		scheduleUpdate();
	}

	public void scale(double x, double y, double z)
	{
		double[][] scale = new double[4][4];
		scale[0][0] = x;
		scale[1][1] = y;
		scale[2][2] = z;
		scale[3][3] = 1;
		this.scale = new Matrix(this.scale.multiplyMatrix(scale));
		scheduleUpdate();
	}

	//due to the way the coordinates are laid out, pitch, yaw, roll currently looks wrong
	//TODO change coordinates so that x/z are horizontal and y is vertical
	@Override
	public void rotate(double pitch, double yaw, double roll)
	{
		pitch(pitch);
		yaw(yaw);
		roll(roll);
	}

	@Override
	public void pitch(double angle) {
		if (pitch != angle) {
			pitch = angle;
			scheduleUpdate();
		}
	}

	@Override
	public void yaw(double angle) {
		if (yaw != angle) {
			yaw = angle;
			scheduleUpdate();
		}
	}

	@Override
	public void roll(double angle) {
		if (roll != angle) {
			roll = angle;
			scheduleUpdate();
		}
	}

	protected abstract void createShape();

	public double[] findCentre() {
		double[] centre = new double[]{0.5, 0.5, 0.5};
		updateMatrix();
		return transform(centre);
	}

	private void updateMatrix()
	{
		double[][] pitchRot = new double[4][4];
		double cosPitch = Math.cos(pitch), sinPitch = Math.sin(pitch);
		pitchRot[0][0] = 1;
		pitchRot[1][1] = cosPitch;
		pitchRot[2][2] = cosPitch;
		pitchRot[3][3] = 1;
		pitchRot[2][1] = -sinPitch;
		pitchRot[1][2] = sinPitch;
		this.xRotation = new Matrix(pitchRot);

		double[][] yawRot = new double[4][4];
		double cosYaw = Math.cos(yaw), sinYaw = Math.sin(yaw);
		yawRot[0][0] = cosYaw;
		yawRot[1][1] = 1;
		yawRot[2][2] = cosYaw;
		yawRot[3][3] = 1;
		yawRot[0][2] = sinYaw;
		yawRot[2][0] = -sinYaw;
		this.yRotation = new Matrix(yawRot);

		double[][] rollRot = new double[4][4];
		double cosRoll = Math.cos(roll), sinRoll = Math.sin(roll);
		rollRot[0][0] = cosRoll;
		rollRot[1][1] = cosRoll;
		rollRot[2][2] = 1;
		rollRot[3][3] = 1;
		rollRot[0][1] = -sinRoll;
		rollRot[1][0] = sinRoll;
		this.zRotation = new Matrix(rollRot);
	}

	private double[] transform(int i)
	{
		double[] point = relativePoints.get(i);
		return transform(point);
	}

	private double[] transform(double[] point)
	{
		point = scale.pointMultiply(point);
		point = xRotation.pointMultiply(point);
		point = yRotation.pointMultiply(point);
		point = zRotation.pointMultiply(point);
		point = translation.pointMultiply(point);
		return point;
	}

	// gives back rotated relativePoints x, y, z :0, 1, 2
	public void update() {
		if (needsUpdate) {
			needsUpdate = false;

			updateMatrix();

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
