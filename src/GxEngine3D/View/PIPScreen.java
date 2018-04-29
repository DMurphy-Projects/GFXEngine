package GxEngine3D.View;

import DebugTools.TextOutput;
import GxEngine3D.Model.Polygon2D;

import javax.xml.soap.Text;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class PIPScreen extends Screen {

    List<PIPView> views = new ArrayList<>();

    public void addView(PIPView view)
    {
        views.add(view);
    }

    public void paint(Graphics g) {
        for (PIPView view:views)
        {
            ViewHandler handler = view.viewHandler;
            PolygonIterator it = handler.getScene().getIterator(handler);
            int width = view.viewHandler.view.getWidth(), height = view.viewHandler.view.getHeight();

            g.setColor(new Color(140, 180, 180));
            g.fillRect(view.position[0], view.position[1], width,  height);

            g.setColor(Color.BLACK);
            g.drawRect(view.position[0], view.position[1], width,  height);

            g.setClip(view.position[0], view.position[1], width,  height);
            if (it != null)
            {
                while (it.hasNext()) {
                    Polygon2D p = it.next();

                    p.getPolygon().translate(view.position[0], view.position[1]);
                    p.drawPolygon(g);
                }
            }
        }
        g.setClip(null);
        drawMouseAim(g);
    }
}
