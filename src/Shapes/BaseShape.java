package Shapes;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.util.ArrayList;

import GxEngine3D.CalculationHelper.MatrixHelper;
import GxEngine3D.Lighting.AltLighting;
import GxEngine3D.Lighting.ILightingStrategy;
import GxEngine3D.Model.Matrix.Matrix;
import GxEngine3D.Model.Polygon3D;
import GxEngine3D.Model.RefBoolean;
import GxEngine3D.Model.RefPoint3D;
import Shapes.Split.ISplitStrategy;
import Shapes.Split.SplitIntoTriangles;

//NOTE:
//--when creating a shape, add points using addPoint()
//--when creating edges or polygons, be sure to reference "points" NOT "relativePoints"
//--shapes relative points should be centred around {0.5, 0.5, 0.5}
//--as of now, relative points do NOT need to be normalised, but should start at {0, 0, 0}
public abstract class BaseShape implements IShape, IDrawable, IManipulable {

	protected double pitch = 0, yaw = 0, roll = 0, x = 0, y = 0, z = 0;
	Matrix scale, pitchRotation, yawRotation, rollRotation, translation;

	protected Color c;

	static int id = 0;
	int curId;
	private RefBoolean needsUpdate, pitchUpdate, yawUpdate, rollUpdate, translateUpdate;

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

		double[][] identity = MatrixHelper.setupIdentityMatrix();

		needsUpdate = new RefBoolean(true);

		pitchUpdate = new RefBoolean(true);
		yawUpdate = new RefBoolean(true);
		rollUpdate = new RefBoolean(true);

		translateUpdate = new RefBoolean(true);

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
		//set global update flag
		if (!needsUpdate.get()) {
			needsUpdate.set(true);
		}
	}
	protected void scheduleUpdate(RefBoolean b)
	{
		scheduleUpdate();
		//set the specific update flag
		if (!b.get())
		{
			b.set(true);
		}
	}

	public void translate(double x, double y, double z)
	{
		//if we're moving the object at all
		if (x+y+z != 0) {
			this.x += x;
			this.y += y;
			this.z += z;
			scheduleUpdate(translateUpdate);
		}
	}

	@Override
	public void absoluteTranslate(double x, double y, double z) {
		//if something is different
		if (this.x != x || this.y != y || this.z != z) {
			this.x = x;
			this.y = y;
			this.z = z;
			scheduleUpdate(translateUpdate);
		}
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

	@Override
	public void absoluteScale(double x, double y, double z) {
		double[][] scale = new double[4][4];
		scale[0][0] = x;
		scale[1][1] = y;
		scale[2][2] = z;
		scale[3][3] = 1;
		this.scale = new Matrix(scale);
		scheduleUpdate();
	}

	//due to the way the coordinates are laid out, pitch, yaw, roll currently looks wrong
	//TODO change coordinates so that x/z are horizontal and y is vertical
	@Override
	public void rotate(double pitch, double yaw, double roll)
	{
		pitch(this.pitch + pitch);
		yaw(this.yaw + yaw);
		roll(this.roll + roll);
	}

	@Override
	public void absoluteRotate(double pitch, double yaw, double roll) {
		pitch(pitch);
		yaw(yaw);
		roll(roll);
	}

	public void pitch(double angle) {
		if (pitch != angle) {
			pitch = angle;
			scheduleUpdate(pitchUpdate);
		}
	}

	public void yaw(double angle) {
		if (yaw != angle) {
			yaw = angle;
			scheduleUpdate(yawUpdate);
		}
	}

	public void roll(double angle) {
		if (roll != angle) {
			roll = angle;
			scheduleUpdate(rollUpdate);
		}
	}

	protected abstract void createShape();

	public double[] findCentre() {
		double[] centre = new double[]{0.5, 0.5, 0.5};
		updateMatrix();
		return transform(centre);
	}

	//only update the things that change
	private void updateMatrix()
	{
		if (pitchUpdate.get()) {
			pitchUpdate.set(false);
			this.pitchRotation = new Matrix(MatrixHelper.setupPitchRotation(pitch));
		}
		if(yawUpdate.get()) {
			yawUpdate.set(false);
			this.yawRotation = new Matrix(MatrixHelper.setupYawRotation(yaw));
		}
		if (rollUpdate.get()) {
			rollUpdate.set(false);
			this.rollRotation = new Matrix(MatrixHelper.setupRollRotation(roll));
		}
		if (translateUpdate.get()) {
			translateUpdate.set(false);
			this.translation = new Matrix(MatrixHelper.setupTranslateMatrix(x, y, z));
		}
	}

	private double[] transform(int i)
	{
		double[] point = relativePoints.get(i);
		return transform(point);
	}

	private double[] transform(double[] point)
	{
		//TODO multiply matrices together first then each point
		point = scale.pointMultiply(point);
		point = pitchRotation.pointMultiply(point);
		point = yawRotation.pointMultiply(point);
		point = rollRotation.pointMultiply(point);
		point = translation.pointMultiply(point);
		return point;
	}

	// gives back rotated relativePoints x, y, z :0, 1, 2
	public void update() {
		if (needsUpdate.get()) {
			needsUpdate.set(false);

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
