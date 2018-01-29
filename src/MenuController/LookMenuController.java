package MenuController;

import GxEngine3D.Scene;
import Shapes.IShape;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * Created by Dean on 02/01/17.
 */
public class LookMenuController {
    public void updateMenu(JMenu lookMenu, Scene scene, ActionListener _this)
    {
        lookMenu.removeAll();
        int count = 0;
        ArrayList<IShape> shp = scene.getShapes();
        if (shp.size() == 0) {
            JMenuItem menuItem = new JMenuItem("--None--");
            lookMenu.add(menuItem);
        } else
            for (IShape i : shp) {
                JMenuItem menuItem = new JMenuItem(i.toString());
                menuItem.setActionCommand("look:" + count);
                count++;
                menuItem.addActionListener(_this);
                lookMenu.add(menuItem);
            }
    }
}
