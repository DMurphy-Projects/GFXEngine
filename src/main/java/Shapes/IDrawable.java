package Shapes;

import java.awt.*;

/**
 * Created by Dean on 28/12/16.
 */
public interface IDrawable {
    void draw(Graphics g, Polygon p);
    void drawOutlines(Graphics g, Polygon p);
    void drawHighlight(Graphics g, Polygon p);
}
