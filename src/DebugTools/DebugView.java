package DebugTools;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;

public class DebugView extends JPanel {

    int[] buffer;
    int width, height;


    public DebugView(int w, int h)
    {
        width = w;
        height = h;
        setPreferredSize(new Dimension(w, h));
    }

    public void update(int[] b)
    {
        buffer = b;
    }

    public int getIndex(int[] wh)
    {
        return getIndex(wh[0], wh[1]);
    }
    public int getIndex(int w, int h)
    {
        return (h * this.width) + w;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (buffer == null) return;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, width, height, buffer, 0, width);
        g.drawImage(image, 0, 0, null);

//        g.setColor(Color.black);
//        g.fillRect(0, 0, width, height);
//        g.setColor(Color.white);
//        for (int x=0;x<width;x++)
//        {
//            for (int y=0;y<height;y++)
//            {
//                int data = buffer[(y*width) +x];
//                if (data > 0)
//                {
//                    g.fillRect(x, y, 1, 1);
//                }
//            }
//        }
    }
}
