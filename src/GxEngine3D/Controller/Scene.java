package GxEngine3D.Controller;

import java.util.*;

import GxEngine3D.CalculationHelper.Matrix;
import GxEngine3D.CalculationHelper.PlaneCalc;
import GxEngine3D.CalculationHelper.VectorCalc;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Camera.ICameraEventListener;
import GxEngine3D.Controller.SplitManager;
import GxEngine3D.Lighting.Light;
import GxEngine3D.Model.*;
import GxEngine3D.Ordering.IOrderStrategy;
import GxEngine3D.Ordering.OrderPolygon;
import GxEngine3D.View.PolygonIterator;
import Shapes.IShape;

public class Scene extends SplitManager implements ICameraEventListener{

	private ArrayList<IShape> shapes = new ArrayList<IShape>();

	private ArrayList<Polygon3D> polygons = new ArrayList<Polygon3D>();
	private ArrayList<Polygon3D> splitPolygons = new ArrayList<>();

	private Camera cam;
	
	Light lightSource;
	IOrderStrategy orderStrategy;

	private boolean mNeedReDraw = true, needsUpdate = false;
	
	public Scene(Camera c, Light ls, double size) {
		super(size);
		cam = c;
		cam.add(this);
		lightSource = ls;
		orderStrategy = new OrderPolygon();
	}

	public void scheduleUpdate()
	{
		if (!needsUpdate)
		{
			needsUpdate = true;
			//if we're changing the polygons we should redraw them
			scheduleRedraw();
		}
	}

	public void scheduleRedraw()
	{
		if (!mNeedReDraw)
		{
			mNeedReDraw = true;
		}
	}

	public void addObject(IShape s) {
		this.scheduleSplit();
		shapes.add(s);
		scheduleUpdate();
	}
	
	public ArrayList<IShape> getShapes()
	{
		return (ArrayList<IShape>) shapes.clone();
	}

	public PolygonIterator getIterator()
	{
		//we only need to order the polygons when we're about to draw them instead of every time we update the polygons
		ArrayList<Polygon3D> copy = (ArrayList<Polygon3D>) splitPolygons.clone();
		int[] o = orderStrategy.order(cam.From(), copy);
		setPolyHover(copy, o);
		return new PolygonIterator(copy, o);
	}
	
	public void update() {
		boolean redraw = mNeedReDraw, update = needsUpdate;
		mNeedReDraw = false;
		needsUpdate = false;
		if (update){
			//somethings changed but we don't know what, either:
			//-a shape was added
			//-(future)a shape was removed
			polygons.clear();
			for (IShape s : shapes) {
				for (Polygon3D p : s.getShape()) {
					polygons.add(p);
				}
			}
		}
		//polygons being redrawn is based on whether they've changed onscreen
		//this can happen either:
		//-moving the camera
		//-moving the polygon
		//so we still need to update polygon in case its trying to move
		if (redraw) {
			for (IShape s : shapes)
			{
				s.update();
			}
		}
		if (update) {
			updateSplitting();
		}
		if (redraw) {
			cam.setup();
			lightSource.updateLighting();
			for (Polygon3D poly : splitPolygons) {
				poly.updatePolygon(cam, lightSource);
			}
		}
	}

	private void setPolyHover(ArrayList<Polygon3D> polys, int[] order) {
		Polygon3D dp;
		for (int i = polys.size()-1; i >= 0; i--) {
		dp = polys.get(order[i]);
		if (dp.canDraw())
			if (dp.get2DPoly().MouseOver()) {
				dp.getBelongsTo().hover(dp);
				break;
			}
		}
	}

	@Override
	public void updateSplitting() {
		splitPolygons = (ArrayList<Polygon3D>) polygons.clone();
//		System.out.println("S "+splitPolygons.size());
		for (int i=0;i<splitPolygons.size();i++)
		{
			//find line intersection between the planes
			Polygon3D p1 = splitPolygons.get(i);
			if (p1.getShape().length <= 2) continue;//this is a line or a point
			Plane plane01 = new Plane(p1);

			ArrayList<Polygon3D> copy = (ArrayList<Polygon3D>) splitPolygons.clone();
			//items behind i have been entirely checked so no need to keep checking them
			for (int ii=i+1;ii<copy.size();ii++)
			{
				Polygon3D p2 = copy.get(ii);
				if (p2.getShape().length <= 2) continue;
				Plane plane02 = new Plane(p2);
				//the same planes can have extremely small differences that the matrix see's them as different planes
				if (VectorCalc.v3_v3_eqauls(plane01.getNV().toArray(), plane02.getNV().toArray())) continue;
				Matrix m = new Matrix(2, 4);
				m.addEqautionOfPlane(plane01);
				m.addEqautionOfPlane(plane02);
				m.gaussJordandElimination();
				m.determineSolution();
				if (m.getSolutionType() == Matrix.SolutionType.LINE)
				{
					//find point intersection between the line intersection and the edges of the poly
					SplittingPackage[] line = splitPolygon(p1, m);
					if (line != null) {
						//find the point intersection for the other poly
						SplittingPackage[] line02 = splitPolygon(p2, m);
						if (line02 != null) {
							//if both exist, then split p1, p2
							//TODO splits are generating polygons that are the same as the original
							Polygon3D[] splits01 = p1.splitAlong(line);
							Polygon3D[] splits02 = p2.splitAlong(line02);
							splitPolygons.remove(i);
							splitPolygons.remove(ii-1);//since we removed i, we need to adjust ii by 1 also
							for (Polygon3D p : splits01) {
								splitPolygons.add(i, p);
							}
							i--;//since we removed the plane we split, we need to adjust the first iterator
							for (Polygon3D p:splits02)
							{
								splitPolygons.add(p);
							}
							break;//we split the polygon and it no longer exists so this iteration needs to stop
						}
					}
				}
			}
			if (splitPolygons.size() > 100) break;
		}
//		System.out.println("E "+splitPolygons.size());
	}

	private boolean linesIntersects(SplittingPackage[] line01, SplittingPackage[] line02)
	{
		boolean b1 = VectorCalc.p3_in_line_seg(line01[0].getPoint(), line01[1].getPoint(), line02[0].getPoint());
		boolean b2 = VectorCalc.p3_in_line_seg(line01[0].getPoint(), line01[1].getPoint(), line02[1].getPoint());
		boolean b3 = VectorCalc.p3_in_line_seg(line02[0].getPoint(), line01[1].getPoint(), line01[0].getPoint());
		boolean b4 = VectorCalc.p3_in_line_seg(line02[0].getPoint(), line01[1].getPoint(), line01[1].getPoint());

		return (b1 || b2) && (b3 || b4);
	}


	//returns true if the entire poly is above or below the plane
	private boolean sameSide(Plane p, Polygon3D poly)
	{
		double[] nV = p.getNV().toArray();
		int side = PlaneCalc.whichSide(poly.getShape(), nV, p.getP());
		return side != 0;
	}

	private SplittingPackage[] splitPolygon(Polygon3D poly, Matrix lineIntersect)
	{
		RefPoint3D[] shape = poly.getShape();
		ArrayList<SplittingPackage> points = new ArrayList<>();
		int i, j;
		for (i = 0, j = shape.length-1; i < shape.length; j = i++)
		{
			double[] p = splitEdge(shape[i].toArray(), shape[j].toArray(), lineIntersect);
			if (p != null)
			{
				//check if the intersection is on the line segment
				if (VectorCalc.p3_in_line_seg(shape[i].toArray(),
						shape[j].toArray(), p))
				{
					points.add(new SplittingPackage(p, i));
				}
			}
		}
		if (points.size() >= 2)
		{
			//makes proper order
			if (points.get(0).getIndex() > points.get(1).getIndex())
			{
				return new SplittingPackage[]{
						points.get(1),
						points.get(0)
				};
			}
			else
			{
				return new SplittingPackage[]{
						points.get(0),
						points.get(1)
				};
			}
		}
		//not enough intersection points were found
		return null;
	}

	private double[] splitEdge(double[] e1, double[] e2, Matrix lineIntersect)
	{
		//we need to add another line so +2 length required
		Matrix edgeIntersect = new Matrix(lineIntersect.getRows() + 2, 4);
		edgeIntersect.addMatrixOfEqautions(lineIntersect);
		edgeIntersect.addEqautionOfLine(e1, e2);
		edgeIntersect.gaussJordandElimination();
		edgeIntersect.determineSolution();
		if (edgeIntersect.getSolutionType() == Matrix.SolutionType.POINT) {
			return edgeIntersect.getPointSolution();
		}
		else
		{
			//there was no intersect
			return null;
		}
	}

	@Override
	public void onLook(double v, double h) {
		scheduleRedraw();
	}

	@Override
	public void onMove(double x, double y, double z) {
		scheduleRedraw();
	}
}
