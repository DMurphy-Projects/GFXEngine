package Programs;

import GxEngine3D.CalculationHelper.Matrix;
import GxEngine3D.Model.Plane;
import GxEngine3D.Model.RefPoint3D;
import Shapes.Shape2D.Sqaure;

import java.awt.*;
import java.util.ArrayList;

public class SplittingTest {
    public static void main(String[] args)
    {
        Sqaure sq01 = new Sqaure(0, 0, 2, 5, Color.white);
        Sqaure sq02 = new Sqaure(0, 0, 2, 5, Color.white);
        sq02.roll(Math.toRadians(90));
        sq02.pitch(Math.toRadians(20));
        sq02.yaw(Math.toRadians(45));
        sq02.update();

        Matrix m = new Matrix(2, 4);
        m.addEqautionOfPlane(new Plane(sq01.getShape().get(0)));
        m.addEqautionOfPlane(new Plane(sq02.getShape().get(0)));
        m.gaussJordandElimination();

        m.determineSolution();
        m.getSolutionType();

        ArrayList<double[]> points = new ArrayList<>();
        for (RefPoint3D[] edge:sq02.getEdges())
        {
            //two lines so has to be 4 length
            Matrix edgeIntersect = new Matrix(4, 4);
            edgeIntersect.addMatrixOfEqautions(m);
            edgeIntersect.addEqautionOfLine(edge[0].toArray(), edge[1].toArray());
            edgeIntersect.gaussJordandElimination();
            edgeIntersect.determineSolution();
            if (edgeIntersect.getSolutionType() == Matrix.SolutionType.POINT)
            {
                points.add(edgeIntersect.getPointSolution());
            }
            if (points.size() >= 2)
            {
                break;
            }
        }
        System.out.println();
    }
}
