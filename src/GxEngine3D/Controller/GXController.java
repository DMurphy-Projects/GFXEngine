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

	// TODO fix this once polygon splitting happens
	static boolean outlines = true, hover = true;

	Map<Camera.Direction, Boolean> keys = new HashMap<>();

	ViewController viewController;

	public GXController(ViewController viewCon) {
		viewController = viewCon;
		for (ViewHandler vH:viewCon.getHandlers())
		{
			vH.getCamera().lookAt((BaseShape) vH.getScene().getShapes().get(0));
			vH.getCamera().setup();
		}
	}

	public void update() {
		notifyTick();
		setup();
	}

	public void setup()
	{
		//redraw the view we're currently controlling
		if (isKeyPressed())
		{
			ViewHandler active = viewController.getActive();
			active.getCamera().CameraMovement(keys);
			active.getScene().scheduleRedraw();
		}
		//update scenes that each handler has
		for (ViewHandler vH:viewController.getHandlers())
		{
			vH.update();
		}
		for (Scene s:viewController.getScenes())
		{
			s.updateFinished();
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
			keys.put(Camera.Direction.RIGHT, false);
		if (e.getKeyCode() == KeyEvent.VK_D)
			keys.put(Camera.Direction.DOWN, false);
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_W)
			keys.put(Camera.Direction.UP, true);
		if (e.getKeyCode() == KeyEvent.VK_A)
			keys.put(Camera.Direction.LEFT, true);
		if (e.getKeyCode() == KeyEvent.VK_S)
			keys.put(Camera.Direction.RIGHT, true);
		if (e.getKeyCode() == KeyEvent.VK_D)
			keys.put(Camera.Direction.DOWN, true);
		if (e.getKeyCode() == KeyEvent.VK_O)
			outlines = !outlines;
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
		vH.getCamera().MouseMovement(arg0.getX() - centre[0], arg0.getY() - centre[1]);
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

	public static boolean hasOutlines()
	{
		return outlines;
	}
	public static boolean canHover()
	{
		return hover;
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
	}
}
