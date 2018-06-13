package GxEngine3D.Camera;

import GxEngine3D.CalculationHelper.DistanceCalc;
import GxEngine3D.CalculationHelper.VectorCalc;
import GxEngine3D.Model.Matrix.Matrix;
import Shapes.BaseShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Camera implements ICameraEvent{

	private List<ICameraEventListener> mListeners = new ArrayList<ICameraEventListener>();

	protected double[] viewFrom, prevFrom;
	private double prevVLook = 0, prevHLook = 0;

	protected double pitch = -0, yaw = 0, yawSpeed = 900, pitchSpeed = 2200, moveSpeed = 0.25;
	private double upper = (Math.PI/2)-0.001, lower = (-Math.PI/2)+0.001;

	Matrix cameraMatrix;

	public enum Direction
	{
		UP,
		DOWN,
		LEFT,
		RIGHT
	}

	public Camera(double x, double y, double z) {
		viewFrom = new double[] { x, y, z };
		prevFrom = new double[] { 0, 0, 0 };
	}

	public double[] From() {
		return viewFrom;
	}

	public Matrix getMatrix() {
		return cameraMatrix;
	}

	public double[] getPosition()
	{
		return viewFrom;
	}
	public double[] getDirection()
	{
		double cosPitch = Math.cos(pitch);
		double[] direction = new double[]{
				-cosPitch * Math.sin(yaw),
				Math.sin(pitch),
				-cosPitch * Math.cos(yaw)
		};
		return direction;
	}

	public void setup() {
		double cosPitch = Math.cos(pitch);
		double sinPitch = Math.sin(pitch);
		double cosYaw = Math.cos(yaw);
		double sinYaw = Math.sin(yaw);

		double[] xaxis = { cosYaw, 0, -sinYaw};
		double[] yaxis = { sinYaw * sinPitch, cosPitch, cosYaw * sinPitch};
		double[] zaxis = { sinYaw * cosPitch, -sinPitch, cosPitch * cosYaw};

		double[][] viewMatrix = new double[][]{
				{       xaxis[0],            xaxis[1],            xaxis[2],      -VectorCalc.dot( xaxis, viewFrom ) },
				{       yaxis[0],            yaxis[1],            yaxis[2],      -VectorCalc.dot( yaxis, viewFrom ) },
				{       zaxis[0],            zaxis[1],            zaxis[2],      -VectorCalc.dot( zaxis, viewFrom ) },
				{ 		0,	 				 0, 				  0, 			 1}
		};

		cameraMatrix = new Matrix(viewMatrix);
	}

	public void lookAt(BaseShape s) {
		double[] look = s.findCentre();
		//System.out.println(look[0]+" "+look[1]+" "+look[2]);
		yaw = Math.atan2(viewFrom[0] - look[0], viewFrom[2] - look[2]);

		// get x,y distance
		double xyDist = DistanceCalc.getDistance(new double[] { look[0],
				look[1] }, new double[] { viewFrom[0], viewFrom[2] });
		//get z dist
		double zDist = look[1] - viewFrom[1];
		//xy represents triangle adjacent, z represents triangle opposite
		/*  /|
		   / |
		  /  | z     . = opp/adj
		 /.__|
		   xy
		*/
		double angle = Math.atan2(zDist , xyDist);
		pitch = angle;
	}

	public void MoveTo(double x, double y, double z) {
		prevFrom[0] = viewFrom[0];
		prevFrom[1] = viewFrom[1];
		prevFrom[2] = viewFrom[2];
		viewFrom[0] = x;
		viewFrom[1] = y;
		viewFrom[2] = z;
		notifyMove();
	}

	public void CameraMovement(Map<Direction, Boolean> directions)
	{
		double[] viewVector = getDirection();
		double[] move = new double[3];
		double[] verticalVector = new double[]{0, 1, 0};
		double[] sideViewVector = VectorCalc.cross(viewVector, verticalVector);

		for (Map.Entry<Direction, Boolean> dir:directions.entrySet()) {
			if (dir.getValue()) {
				Direction d = dir.getKey();
				if (d == Direction.UP) {
					move = VectorCalc.add(move, viewVector);
				} else if (d == Direction.DOWN) {
					move = VectorCalc.sub(move, viewVector);
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

		pitch += difY / pitchSpeed;
		yaw += difX / yawSpeed;
		if (pitch >= upper)
			pitch = upper;
		if (pitch <= lower)
			pitch = lower;

		if (prevHLook != yaw || prevVLook != pitch) {
			prevHLook = yaw;
			prevVLook = pitch;
			notifyLook();
		}
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
			e.onLook(yaw, pitch);
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
