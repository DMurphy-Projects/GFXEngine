package GxEngine3D.View.ViewHelper;

import java.awt.*;

public class MouseAim {
    static double aimSight = 4;

    public static void drawMouseAim(Graphics g, int width, int height)
    {
        g.setColor(Color.black);
        g.drawLine((int) (width / 2 - aimSight), (height / 2),
                (int) (width / 2 + aimSight), height / 2);
        g.drawLine(width / 2, (int) (height / 2 - aimSight),
                width / 2, (int) (height / 2 + aimSight));
    }
}
