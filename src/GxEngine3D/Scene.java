package GxEngine3D;

import java.util.ArrayList;

import GxEngine3D.Camera.Camera;
import GxEngine3D.Camera.ICameraEventListener;
import GxEngine3D.Lighting.Light;
import GxEngine3D.Ordering.IOrderStrategy;
import GxEngine3D.Ordering.OrderPolygon;
import Shapes.IShape;

public class Scene extends SplitManager implements ICameraEventListener{

	private ArrayList<IShape> shapes = new ArrayList<IShape>();

	private ArrayList<Polygon3D> polygons = new ArrayList<Polygon3D>();

	private int[] mNewOrder = new int[0];
	private int orderPos = 0;

	private Polygon2D PolygonOver = null;
	private Camera cam;
	
	Light lightSource;
	IOrderStrategy orderStrategy;

	private boolean mNeedReDraw = true;
	
	public Scene(Camera c, Light ls, double size) {
		super(size);
		cam = c;
		cam.add(this);
		lightSource = ls;
		orderStrategy = new OrderPolygon();
	}

	public void scheduleRedraw()
	{
		if (!mNeedReDraw)
		{
			mNeedReDraw = true;
		}
	}

	private void addPolygons(IShape s)
	{
		for (Polygon3D dp : s.getShape()) {
			polygons.add(dp);
		}
		scheduleRedraw();
	}
	public void addObject(IShape s) {
		this.scheduleSplit();
		shapes.add(s);
		scheduleRedraw();
	}
	
	public ArrayList<IShape> getShapes()
	{
		return (ArrayList<IShape>) shapes.clone();
	}

	//splitting taking too long that it tries to draw while adding new splitted polys
	public Polygon2D nextPolygon() {
		if (orderPos > mNewOrder.length - 1)
			return null;
		Polygon2D d = polygons.get(mNewOrder[orderPos]).screenPoly;
		//System.out.println("D "+mNewOrder[orderPos]);
		orderPos++;
		return d;
	}
	
	public void update() {
		boolean redraw = mNeedReDraw;
		mNeedReDraw = false;
		cam.setup();
		lightSource.updateLighting();
		if (redraw){
			polygons.clear();
		}
		updateSplitting();

		//polygons being redrawn is based on whether they've changed onscreen
		//this can happen either:
		//-moving the camera
		//-moving the polygon
		//so we still need to update polygon incase its trying to move
		for (IShape s : shapes)
		{
			s.update();
			if (redraw) {
				for (Polygon3D p : s.getShape()) {
					p.updatePolygon(cam, lightSource);
					polygons.add(p);
				}
			}
		}
		orderPos = 0;
		if (redraw) {
			mNewOrder = orderStrategy.order(cam.From(), shapes, polygons);
		}
		setPolyHover();
	}

	private void setPolyHover() {
		PolygonOver = null;
		Polygon3D dp;
			for (int i = polygons.size()-1; i >= 0; i--) {
			dp = polygons.get(mNewOrder[i]);
			if (dp.draw)
				if (dp.screenPoly.MouseOver()) {
					PolygonOver = dp.screenPoly;
					dp.belongsTo.hover(dp);
					dp.screenPoly.hover();
					break;
				}
		}
	}

	@Override
	public void updateSplitting() {
		if (this.NeedSplit())
		{
			super.updateSplitting();
			for (IShape s:shapes) {
				s.split(this.Size());
			}
			polygons.clear();
			for (IShape s:shapes) {
				addPolygons(s);
			}
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
