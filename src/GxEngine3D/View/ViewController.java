package GxEngine3D.View;

import GxEngine3D.Camera.Camera;
import GxEngine3D.Controller.Scene;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

public class ViewController implements MouseListener{

    List<ViewHandler> handlers = new ArrayList<>();
    List<Scene> uniqueScenes = new ArrayList<>();
    List<JPanel> uniqueViews = new ArrayList<>();
    List<Camera> uniqueCameras = new ArrayList<>();

    ViewHandler active = null;
    //a view consists of the following:
    //-something to render on
    //-something to look from
    //-something to look at
    public ViewHandler add(JPanel v, Camera c, Scene s)
    {
        v.addMouseListener(this);

        ViewHandler vH = new ViewHandler(v, c, s);
        handlers.add(vH);
        //we only want to update each scene/camera once
        addUnique(uniqueScenes, s);
        addUnique(uniqueViews, v);
        addUnique(uniqueCameras, c);
        if (active == null) {
            active = vH;
        }
        return vH;
    }

    private <T> void addUnique(List<T> list, T toAdd)
    {
        for (T t:list)
        {
            if(t.equals(toAdd))
            {
                return;
            }
        }
        list.add(toAdd);
    }

    public ViewHandler getActive()
    {
        return active;
    }

    public List<ViewHandler> getHandlers()
    {
        return handlers;
    }
    public List<JPanel> getViews()
    {
        return uniqueViews;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        for (ViewHandler vH:handlers)
        {
            if (e.getSource().equals(vH.getView()))
            {
                active = vH;
                return;
            }
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
