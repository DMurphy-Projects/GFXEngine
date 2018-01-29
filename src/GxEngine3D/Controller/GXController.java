package GxEngine3D.Controller;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import GxEngine3D.Camera.Camera;
import GxEngine3D.Model.Vector;
import GxEngine3D.View.Screen;
import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;

public class GXController extends GXTickEvent implements KeyListener, MouseListener,
		MouseMotionListener, MouseWheelListener {

	private Scene scene;
	private Camera cam;

	Robot r;

	double drawFPS = 0, mxFPS = 60, sleepTime = 1000.0 / mxFPS,
			lastRefresh = 0, start = System.currentTimeMillis(), lastFPS = 0,
			fpsChecks = 0;
	long repaintTime = 0;

	// TODO fix this once polygon splitting happens
	static boolean outlines = true;

	double moveSpeed = 1;

	boolean[] keys = new boolean[4];

	private ViewHandler vHandler;

	private Screen screen;

	public GXController(Scene s, Camera c, Screen sc, ViewHandler vH) {
		scene = s;
		cam = c;
		vHandler = vH;
		screen = sc;
		try {
			c.lookAt((BaseShape) s.getShapes().get(0));
		} catch (Exception e) {
		}
	}

	public void update() {
		notifyTick();
		setup();
	}
	public void setup()
	{
		CameraMovement(cam.From(), cam.To());

		// Updates each polygon for this camera position, includes draw order
		scene.update();

		screen.repaint();

		sleep();
	}

	private void sleep() {
		long timeSLU = (long) (System.currentTimeMillis() - lastRefresh);

		fpsChecks++;
		if (fpsChecks >= 15) {
			drawFPS = fpsChecks
					/ ((System.currentTimeMillis() - lastFPS) / 1000.0);
			lastFPS = System.currentTimeMillis();
			fpsChecks = 0;
		}

		if (timeSLU < 1000.0 / mxFPS) {
			try {
				Thread.sleep((long) (1000.0 / mxFPS - timeSLU));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		lastRefresh = System.currentTimeMillis();
	}

	private void CameraMovement(double[] from, double[] to) {
		Vector ViewVector = new Vector(to[0] - from[0], to[1] - from[1], to[2]
				- from[2]);
		double xMove = 0, yMove = 0, zMove = 0;
		Vector VerticalVector = new Vector(0, 0, 1);
		Vector SideViewVector = ViewVector.crossProduct(VerticalVector);

		if (keys[0]) {
			xMove += ViewVector.X();
			yMove += ViewVector.Y();
			zMove += ViewVector.Z();
		}

		if (keys[2]) {
			xMove -= ViewVector.X();
			yMove -= ViewVector.Y();
			zMove -= ViewVector.Z();
		}

		if (keys[1]) {
			xMove += SideViewVector.X();
			yMove += SideViewVector.Y();
			zMove += SideViewVector.Z();
		}

		if (keys[3]) {
			xMove -= SideViewVector.X();
			yMove -= SideViewVector.Y();
			zMove -= SideViewVector.Z();
		}

		Vector MoveVector = new Vector(xMove, yMove, zMove);
		cam.MoveTo(from[0] + MoveVector.X() * moveSpeed, from[1] + MoveVector.Y()
				* moveSpeed, from[2] + MoveVector.Z() * moveSpeed);
	}

	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_W)
			keys[0] = false;
		if (e.getKeyCode() == KeyEvent.VK_A)
			keys[1] = false;
		if (e.getKeyCode() == KeyEvent.VK_S)
			keys[2] = false;
		if (e.getKeyCode() == KeyEvent.VK_D)
			keys[3] = false;
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_W)
			keys[0] = true;
		if (e.getKeyCode() == KeyEvent.VK_A)
			keys[1] = true;
		if (e.getKeyCode() == KeyEvent.VK_S)
			keys[2] = true;
		if (e.getKeyCode() == KeyEvent.VK_D)
			keys[3] = true;
		if (e.getKeyCode() == KeyEvent.VK_O)
			outlines = !outlines;
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
			System.exit(0);
	}

	public void mouseWheelMoved(MouseWheelEvent arg0) {
		vHandler.doZoom(arg0.getUnitsToScroll());
	}

	public void mousePressed(MouseEvent arg0) {
	}

	public void mouseDragged(MouseEvent arg0) {
		cam.MouseMovement(arg0.getX(), arg0.getY());
		CenterMouse();
	}

	public void mouseMoved(MouseEvent arg0) {
		cam.MouseMovement(arg0.getX(), arg0.getY());
		CenterMouse();
	}

	public void CenterMouse() {
		try {
			r = new Robot();
			r.mouseMove(vHandler.CenterScreenX(), vHandler.CenterScreenY());
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

	public static boolean hasOutlines()
	{
		return outlines;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub
	}
}
