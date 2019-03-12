package DebugTools;

import GxEngine3D.Helper.FrameHelper;

import javax.swing.*;
import java.awt.*;

//is a quick/easy way of visualising polygons for the use od debugging
public class PaintPad {

    public enum Mode
    {
        Default, Centre
    }

    JFrame frame;
    JPanel panel;

    int xOffset = 0, yOffset = 0, width, height;

    Graphics gfx;

    public PaintPad(int width, int height)
    {
        panel = new JPanel();
        panel.setPreferredSize(new Dimension(width, height));
        frame = FrameHelper.setupFrame(panel, "PaintPad");
        frame.setVisible(true);

        this.width = width;
        this.height = height;
    }

    public void setMode(Mode m)
    {
        switch (m)
        {
            case Default:
                xOffset = 0;
                yOffset = 0;
                break;
            case Centre:
                xOffset = width / 2;
                yOffset = height / 2;
                break;
        }
    }

    public void init()
    {
        gfx = panel.getGraphics();
        gfx.setColor(Color.WHITE);
        gfx.fillRect(0, 0, width, height);

        gfx.translate(xOffset, yOffset);
    }

    public void drawPolygon(Polygon p, Color c, boolean fill)
    {
        gfx.setColor(c);
        if (fill)
        {
            gfx.fillPolygon(p);
        }
        else {
            gfx.drawPolygon(p);
        }
    }

    public void drawPolygonVertices(Polygon p, Color c,  boolean label, boolean fill)
    {
        int count = 0;
        for (int i=0;i<p.npoints;i++)
        {
            String labelString = "";
            if (label) {labelString += count++;}
            drawPoint(p.xpoints[i], p.ypoints[i],  c, 6, labelString, fill);
        }
    }

    public void drawPoint(int x, int y, Color c, int diam, String label, boolean fill)
    {
        gfx.setColor(c);
        gfx.translate(-diam/2, -diam/2);
        if (fill)
        {
            gfx.fillOval(x, y, diam,diam);
            gfx.drawString(label, x, y);
        }
        else
        {
            gfx.drawOval(x, y, diam,diam);
            gfx.drawString(label, x, y);
        }
        gfx.translate(diam/2, diam/2);
    }

    public Polygon createPolygon(double[][] poly, double xScale, double yScale)
    {
        Polygon p = new Polygon();
        for (double[] point: poly)
        {
            p.addPoint((int)(point[0]*xScale), (int) (point[1]*yScale));
        }
        return p;
    }

    public void offset(int x, int y)
    {
        gfx.translate(x, y);
    }

    public void finish()
    {
        gfx.finalize();
        frame.invalidate();
    }
}
