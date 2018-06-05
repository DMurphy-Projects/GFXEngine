package GxEngine3D.View.PIP;

import GxEngine3D.View.ViewHandler;

import javax.swing.*;
import java.awt.*;

public abstract class PIPView extends JPanel {
    int[] offset;
    protected ViewHandler vH;

    public PIPView(int[] o)
    {
        offset = o;
    }

    public void setViewHandler(ViewHandler v)
    {
        vH = v;
    }

   protected void render(Graphics gfx) {
       int w = vH.getView().getWidth();
       int h = vH.getView().getHeight();

       gfx.setColor(new Color(140, 180, 180));
       gfx.fillRect(offset[0], offset[1], w,  h);
       gfx.setColor(Color.BLACK);
       gfx.drawRect(offset[0], offset[1], w,  h);

       gfx.setClip(offset[0], offset[1], w, h);
       gfx.translate(offset[0], offset[1]);
   }
}
