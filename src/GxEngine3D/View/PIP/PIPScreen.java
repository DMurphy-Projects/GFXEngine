package GxEngine3D.View.PIP;

import GxEngine3D.View.ViewHelper.InvisibleMouse;
import GxEngine3D.View.ViewHelper.MouseAim;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PIPScreen extends JPanel {

    List<PIPView> views = new ArrayList<>();

    public PIPScreen()
    {
        setFocusable(true);
        setCursor(InvisibleMouse.createCursor());
        setIgnoreRepaint(true);
    }

    public void addView(PIPView view)
    {
        views.add(view);
    }

    public void paint(Graphics g) {
        for (PIPView view:views)
        {
            view.render(g);
        }
        g.setClip(null);
        MouseAim.drawMouseAim(g, getWidth(), getHeight());
    }
}
