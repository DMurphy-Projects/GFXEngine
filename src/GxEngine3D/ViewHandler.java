package GxEngine3D;

import javax.swing.JPanel;

public class ViewHandler {

	private JPanel view;

	private int zoom = 1000, mnZoom = 500, mxZoom = 2500;

	public ViewHandler() {
	}
	
	public void setPanel(JPanel jp){
		view = jp;
	}

	public int CenterX() {
		return (view.getWidth() / 2);
	}

	public int CenterY() {
		return (view.getHeight() / 2);
	}

	public int CenterScreenX() {
		return CenterX() + view.getLocationOnScreen().x;
	}

	public int CenterScreenY() {
		return CenterY() + view.getLocationOnScreen().y;
	}

	public int Zoom() {
		return zoom;
	}
	
	public void doZoom(int direction)
	{
		//System.out.println(direction);
		zoom -= 25 * direction;
		if (zoom < mnZoom)
			zoom = mnZoom;
		else if (zoom > mxZoom)
			zoom = mxZoom;
	}

}
