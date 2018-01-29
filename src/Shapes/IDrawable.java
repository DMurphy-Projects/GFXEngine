package Shapes;

import java.awt.*;

/**
 * Created by Dean on 28/12/16.
 */
public interface IDrawable {
    public void draw(Graphics g, Polygon p);
    public void drawOutlines(Graphics g, Polygon p);
    public void drawHighlight(Graphics g, Polygon p);
}
