package GxEngine3D.View;

import DebugTools.TextOutput;
import GxEngine3D.Model.Polygon2D;
import GxEngine3D.Controller.Scene;
import GxEngine3D.Model.Polygon3D;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;


//TODO multiple light sources
//TODO make sphere better
//TODO make toolbar, add objects, control light, debug options, etc
public class Screen extends JPanel {
	
	double aimSight = 4;
	
	ViewHandler vH;
	
	public Screen() {
		setFocusable(true);

		invisibleMouse();
		setIgnoreRepaint(true);
	}

	public void setHandler(ViewHandler v)
	{
		vH = v;
	}

	@Override
	public void paint(Graphics g) {
		//draws polygons
		PolygonIterator it = vH.scene.getIterator(vH);
		if (it != null) {
			// Clear screen and draw background color
			g.setColor(new Color(140, 180, 180));
			g.fillRect(0, 0, getWidth(), getHeight());
			while (it.hasNext()) {
				Polygon2D p = it.next();
				p.drawPolygon(g);
			}
			// draw the cross in the centre of the screen
			drawMouseAim(g);
		}
	}
	
	void invisibleMouse() {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		BufferedImage cursorImage = new BufferedImage(1, 1,
				BufferedImage.TRANSLUCENT);
		Cursor invisibleCursor = toolkit.createCustomCursor(cursorImage,
				new Point(0, 0), "InvisibleCursor");
		setCursor(invisibleCursor);
	}

	void drawMouseAim(Graphics g) {
		g.setColor(Color.black);
		g.drawLine((int) (getWidth() / 2 - aimSight), (int) (getHeight() / 2),
				(int) (getWidth() / 2 + aimSight), (int) (getHeight() / 2));
		g.drawLine((int) (getWidth() / 2), (int) (getHeight() / 2 - aimSight),
				(int) (getWidth() / 2), (int) (getHeight() / 2 + aimSight));
	}
}
