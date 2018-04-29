package GxEngine3D.View;

import GxEngine3D.Camera.Camera;
import GxEngine3D.Camera.ICameraEventListener;
import GxEngine3D.Controller.Scene;

import javax.swing.JPanel;

public class ViewHandler implements ICameraEventListener{

	JPanel view;
	Scene scene;
	Camera camera;
	boolean needsRedraw = true;

	private int zoom = 1000, mnZoom = 500, mxZoom = 2500;

	public ViewHandler(JPanel v, Camera c, Scene s) {
		view = v;
		camera = c;
		scene = s;

		c.add(this);
	}

	public void update()
	{
		scene.update(this);
	}

	public int[] getCentre()
	{
		return new int[]{
				view.getWidth() / 2,
				view.getHeight() / 2
		};
	}

	public int[] getScreenCentre()
	{
		int[] centre = getCentre();
		return new int[]{
				view.getLocationOnScreen().x + centre[0],
				view.getLocationOnScreen().y + centre[1]
		};
	}

	public int getZoom() {
		return zoom;
	}


	public void setRedraw(boolean b)
	{
		needsRedraw = b;
	}
	public boolean canRedraw()
	{
		return needsRedraw;
	}

	public JPanel getView() {
		return view;
	}

	public Scene getScene() {
		return scene;
	}
	public Camera getCamera()
	{
		return camera;
	}
	
	public void doZoom(int direction)
	{
		zoom -= 25 * direction;
		if (zoom < mnZoom)
			zoom = mnZoom;
		else if (zoom > mxZoom)
			zoom = mxZoom;
	}

	@Override
	public void onLook(double h, double v) {
		needsRedraw = true;
	}

	@Override
	public void onMove(double x, double y, double z) {
		needsRedraw = true;
	}
}
