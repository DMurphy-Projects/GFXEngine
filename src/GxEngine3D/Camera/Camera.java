package GxEngine3D.Camera;

import GxEngine3D.CalculationHelper.DistanceCalc;
import GxEngine3D.CalculationHelper.ProjectionCalc;
import GxEngine3D.CalculationHelper.VectorCalc;
import GxEngine3D.Model.Plane;
import GxEngine3D.Model.Projection;
import GxEngine3D.Model.Vector;
import Shapes.BaseShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Camera implements ICameraEvent{

	private List<ICameraEventListener> mListeners = new ArrayList<ICameraEventListener>();

	private Vector viewVector, rotationVector, directionVector;
	public Plane P;
	public Vector W1, W2;
	private Projection focusPos;

	protected double[] viewFrom, viewTo, prevFrom;
	private double prevVLook = 0, prevHLook = 0;

	protected double vLook = -0, hLook = 0, hSpeed = 900, vSpeed = 2200, moveSpeed = 1;
	private double upper = (Math.PI/2)-0.001, lower = (-Math.PI/2)+0.001;

	public enum Direction
	{
		UP,
		DOWN,
		LEFT,
		RIGHT
	}

	public Camera(double x, double y, double z) {
		viewFrom = new double[] { x, y, z };
		viewTo = new double[] { 0, 0, 0 };
		updateView();
		prevFrom = new double[] { 0, 0, 0 };
	}

	public double[] From() {
		return viewFrom;
	}

	public double[] To() {
		return viewTo;
	}

	public Projection Focus() {
		return focusPos;
	}
	
	public double[] lookingAt()
	{
		return viewTo;
	}
	public double[] position()
	{
		return viewFrom;
	}
	public double[] direction()
	{
		return VectorCalc.sub(viewTo, viewFrom);
	}

	public void setup() {
		viewVector = new Vector(VectorCalc.sub(viewTo, viewFrom));
		directionVector = new Vector(1, 1, 1);
		Vector planeVector1 = viewVector.crossProduct(directionVector);
		Vector planeVector2 = viewVector.crossProduct(planeVector1);
		P = new Plane(planeVector1, planeVector2, viewTo);

		rotationVector = ProjectionCalc.getRotationVector(viewFrom, viewTo);
		W1 = viewVector.crossProduct(rotationVector);
		W2 = viewVector.crossProduct(W1);

		focusPos = ProjectionCalc.calculateFocus(viewFrom, viewTo[0],
				viewTo[1], viewTo[2], W1, W2, P);
	}

	private double map(double iStart, double iEnd, double oStart, double oEnd,
			double in) {
		double slope = 1.0 * (oEnd - oStart) / (iEnd - iStart);
		return oStart + slope * (in - iStart);
	}

	public void lookAt(BaseShape s) {
		double[] look = s.findCentre();
		//System.out.println(look[0]+" "+look[1]+" "+look[2]);
		hLook = Math.atan2(look[0] - viewFrom[0], look[1] - viewFrom[1]);

		// get x,y distance
		double xyDist = DistanceCalc.getDistance(new double[] { look[0],
				look[1] }, new double[] { viewFrom[0], viewFrom[1] });
		//get z dist
		double zDist = look[2] - viewFrom[2];
		//xy represents triangle adjacent, z represents triangle opposite
		/*  /|
		   / |
		  /  | z     . = opp/adj
		 /.__|
		   xy
		*/
		double angle = Math.atan2(zDist , xyDist);
		vLook = angle;
		updateView();
	}

	public void MoveTo(double x, double y, double z) {
		prevFrom[0] = viewFrom[0];
		prevFrom[1] = viewFrom[1];
		prevFrom[2] = viewFrom[2];
		viewFrom[0] = x;
		viewFrom[1] = y;
		viewFrom[2] = z;
		notifyMove();
		updateView();
	}

	public void CameraMovement(Map<Direction, Boolean> directions)
	{
		double[] viewVector = VectorCalc.sub(viewFrom, viewTo);
		double[] move = new double[3];
		double[] verticalVector = new double[]{0, 0, 1};
		double[] sideViewVector = VectorCalc.cross(viewVector, verticalVector);

		for (Map.Entry<Direction, Boolean> dir:directions.entrySet()) {
			if (dir.getValue()) {
				Direction d = dir.getKey();
				if (d == Direction.UP) {
					move = VectorCalc.sub(move, viewVector);
				} else if (d == Direction.DOWN) {
					move = VectorCalc.add(move, viewVector);
				} else if (d == Direction.LEFT) {
					move = VectorCalc.sub(move, sideViewVector);
				} else if (d == Direction.RIGHT) {
					move = VectorCalc.add(move, sideViewVector);
				}
			}
		}

		move = VectorCalc.add(viewFrom, VectorCalc.mul_v_d(move, moveSpeed));
		MoveTo(move[0], move[1], move[2]);
	}
	public void MouseMovement(double NewMouseX, double NewMouseY) {
		double difX = NewMouseX;
		double difY = NewMouseY;

		vLook += difY / vSpeed;
		hLook += difX / hSpeed;
		if (vLook >= upper)
			vLook = upper;
		if (vLook <= lower)
			vLook = lower;

		if (prevHLook != hLook || prevVLook != vLook) {
			prevHLook = hLook;
			prevVLook = vLook;
			updateView();
			notifyLook();
		}
	}

	void updateView() {	
		double r = Math.cos(vLook);
		viewTo[0] = viewFrom[0] + (r * Math.sin(hLook));
		viewTo[1] = viewFrom[1] + (r * Math.cos(hLook));
		viewTo[2] = viewFrom[2] + Math.sin(vLook);
	}


	@Override
	public void notifyMove() {
		//if are different
		if (prevFrom[0] != viewFrom[0] ||
				prevFrom[1] != viewFrom[1] ||
				prevFrom[2] != viewFrom[2]) {
			for (ICameraEventListener e : mListeners) {
				e.onMove(viewFrom[0], viewFrom[1], viewFrom[2]);
			}
		}
	}

	@Override
	public void notifyLook() {
		for (ICameraEventListener e : mListeners) {
			e.onLook(hLook, vLook);
		}
	}

	@Override
	public void add(ICameraEventListener e) {
		mListeners.add(e);
	}

	@Override
	public void remove(ICameraEvent e) {
		mListeners.remove(e);
	}
}
