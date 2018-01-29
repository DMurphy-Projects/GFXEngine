package Shapes.Split;

import GxEngine3D.Model.Polygon3D;
import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;

import java.awt.*;
import java.util.ArrayList;

/**
 * Created by Dean on 08/01/17.
 */
public interface ISplitStrategy {
    public void split(double maxSize, ArrayList<Polygon3D> polys, Color c, ViewHandler vH, BaseShape bTo);
}
