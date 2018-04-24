package GxEngine3D.View;

import GxEngine3D.Model.Polygon2D;
import GxEngine3D.Controller.Scene;

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
	
	Scene scene;
	
	public Screen(Scene s) {
		scene = s;
		
		setFocusable(true);

		invisibleMouse();
		setIgnoreRepaint(true);
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		// Clear screen and draw background color
		g.setColor(new Color(140, 180, 180));
		g.fillRect(0, 0, (int) getWidth(), (int) getHeight());
		//draws polygons
		PolygonIterator it = scene.getIterator();
		while(it.hasNext())
		{
			Polygon2D p = it.next().get2DPoly();
			p.drawPolygon(g);
		}

		// draw the cross in the centre of the screen
		drawMouseAim(g);
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
