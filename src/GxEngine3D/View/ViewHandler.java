package GxEngine3D.View;

import GxEngine3D.Camera.Camera;
import GxEngine3D.Camera.ICameraEventListener;
import GxEngine3D.Controller.GXTickEvent;
import GxEngine3D.Controller.ITickListener;
import GxEngine3D.Controller.Scene;
import GxEngine3D.Model.Matrix.Matrix;

import javax.swing.JPanel;

public class ViewHandler implements ICameraEventListener, ITickListener {

	JPanel view;
	Scene scene;
	Camera camera;
	boolean needsRedraw = true;

	boolean hasOutlines = true, canHover = true;

	private double zoom = 1, mnZoom = 1, mxZoom = 10;

	Matrix projectionMatrix;

	public ViewHandler(JPanel v, Camera c, Scene s) {
		view = v;
		camera = c;
		scene = s;

		c.add(this);
	}

	//this should be the last thing to update, always
	public void update()
	{
		updateMatrix();
		scene.update(this);
	}

	private double[] gluPerspective(double angleOfView, double imageAspectRatio, double n, double f)
	{
		double scale = (Math.tan(angleOfView * 0.5 * Math.PI / 180) * n) / zoom;
		double r = imageAspectRatio * scale;
		double l = -r;
		double t = scale;
		double b = -t;
		return new double[]{b, t, l, r};
	}

	private Matrix glFrustum(double b, double t, double l, double r,
			double n, double f)
	{
		double[][] m = new double[4][4];
		m[0][0] = 2 * n / (r - l);

		m[1][1] = 2 * n / (t - b);

		m[0][2] = (r + l) / (r - l);
		m[1][2] = (t + b) / (t - b);
		m[2][2] = -(f + n) / (f - n);
		m[3][2] = -1;

		m[2][3] = -2 * f * n / (f - n);

		return new Matrix(m);
	}

	private void updateMatrix()
	{
		double angleOfView = 90;
		double near = 0.1f;
		double far = 50;
		double imageAspectRatio = (double)(view.getWidth()) / view.getHeight();

		double[] btlr = gluPerspective(angleOfView, imageAspectRatio, near, far);
		projectionMatrix = glFrustum(btlr[0], btlr[1], btlr[2], btlr[3], near, far);
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

	public double getZoom() {
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

	public Matrix getProjectionMatrix() {
		return projectionMatrix;
	}

	public boolean canHover()
	{
		return canHover;
	}
	public boolean hasOutlines()
	{
		return hasOutlines;
	}
	public void setOutlines(boolean b)
	{
		hasOutlines = b;
	}
	public void setHover(boolean b)
	{
		canHover = b;
	}
	
	public void doZoom(int direction)
	{
		if (direction < 0)
		{
			zoom += 0.1 * zoom;
			if (zoom > mxZoom) {
				zoom = mxZoom;
			}
		}
		else
		{
			zoom -= 0.1 * zoom;
			if (zoom < mnZoom) {
				zoom = mnZoom;
			}
		}
	}

	@Override
	public void onLook(double h, double v) {
		needsRedraw = true;
	}

	@Override
	public void onMove(double x, double y, double z) {
		needsRedraw = true;
	}

	@Override
	public void onTick(GXTickEvent.Type t) {
		updateMatrix();
		scene.update(this);
	}
}
