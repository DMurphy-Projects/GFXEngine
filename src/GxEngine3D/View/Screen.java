package GxEngine3D.View;

import GxEngine3D.Model.Polygon2D;
import GxEngine3D.View.PIP.PIPView;
import GxEngine3D.View.ViewHelper.InvisibleMouse;
import GxEngine3D.View.ViewHelper.MouseAim;

import java.awt.Color;
import java.awt.Graphics;


//TODO multiple light sources
//TODO make sphere better
//TODO make toolbar, add objects, control light, debug options, etc
public class Screen extends PIPView{

	public Screen(int[] o) {
		super(o);

		setFocusable(true);

		invisibleMouse();
		setIgnoreRepaint(true);
	}

	public Screen()
	{
		super(new int[]{0, 0});
		setFocusable(true);

		invisibleMouse();
		setIgnoreRepaint(true);
	}

	@Override
	public void paint(Graphics g) {
		render(g);
	}

	@Override
	protected void render(Graphics gfx) {
		super.render(gfx);

		//draws polygons
		PolygonIterator it = vH.scene.getIterator(vH);
		if (it != null) {
			// Clear screen and draw background color
			gfx.setColor(new Color(140, 180, 180));
			gfx.fillRect(0, 0, getWidth(), getHeight());
			while (it.hasNext()) {
				Polygon2D p = it.next();
				p.drawPolygon(gfx);
			}
			// draw the cross in the centre of the screen
			MouseAim.drawMouseAim(gfx, getWidth(), getHeight());
		}
	}

	void invisibleMouse() {
		setCursor(InvisibleMouse.createCursor());
	}

}
