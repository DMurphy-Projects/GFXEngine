package GxEngine3D.Controller;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.*;

import DebugTools.TextOutput;
import GxEngine3D.Camera.Camera;
import GxEngine3D.View.ViewController;
import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;

import javax.swing.*;

public class GXController extends GXTickEvent implements KeyListener,
		MouseMotionListener, MouseWheelListener {
	Robot r;

	double drawFPS = 0, mxFPS = 60,
			lastRefresh = 0, lastFPS = 0,
			fpsChecks = 0;

	Map<Camera.Direction, Boolean> keys = new HashMap<>();

	ViewController viewController;

	public GXController(ViewController viewCon) {
		viewController = viewCon;
	}

	public void update()
	{
		notifyPreTick();
		notifyTick();
		notifyPostTick();
		//redraw the view we're currently controlling
		if (isKeyPressed())
		{
			ViewHandler active = viewController.getActive();
			active.getCamera().CameraMovement(keys);
			active.getScene().scheduleRedraw();
		}
		for (JPanel p:viewController.getViews())
		{
			p.repaint();
		}
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

	private boolean isKeyPressed()
	{
		return keys.containsValue(true);
	}

	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_W)
			keys.put(Camera.Direction.UP, false);
		if (e.getKeyCode() == KeyEvent.VK_A)
			keys.put(Camera.Direction.LEFT, false);
		if (e.getKeyCode() == KeyEvent.VK_S)
			keys.put(Camera.Direction.DOWN, false);
		if (e.getKeyCode() == KeyEvent.VK_D)
			keys.put(Camera.Direction.RIGHT, false);
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_W)
			keys.put(Camera.Direction.UP, true);
		if (e.getKeyCode() == KeyEvent.VK_A)
			keys.put(Camera.Direction.LEFT, true);
		if (e.getKeyCode() == KeyEvent.VK_S)
			keys.put(Camera.Direction.DOWN, true);
		if (e.getKeyCode() == KeyEvent.VK_D)
			keys.put(Camera.Direction.RIGHT, true);
		if (e.getKeyCode() == KeyEvent.VK_O)
			viewController.getActive().setOutlines(!viewController.getActive().hasOutlines());
		if (e.getKeyCode() == KeyEvent.VK_H)
			viewController.getActive().setHover(!viewController.getActive().canHover());
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
			System.exit(0);
	}

	public void mouseWheelMoved(MouseWheelEvent arg0) {
		viewController.getActive().doZoom(arg0.getUnitsToScroll());
	}

	public void mousePressed(MouseEvent arg0) {
	}

	public void mouseDragged(MouseEvent arg0) {
		viewController.getActive().getCamera().MouseMovement(arg0.getX(), arg0.getY());
		centreMouse();
	}

	public void mouseMoved(MouseEvent arg0) {
		ViewHandler vH = viewController.getActive();
		int[] centre = vH.getCentre();
		vH.getCamera().MouseMovement(centre[0] - arg0.getX(), centre[1] - arg0.getY());
		centreMouse();
	}

	public void centreMouse() {
		try {
			r = new Robot();
			int[] centre = viewController.getActive().getScreenCentre();
			r.mouseMove(centre[0], centre[1]);
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
	}
}
