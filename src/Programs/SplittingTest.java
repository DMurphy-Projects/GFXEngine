package Programs;

import GxEngine3D.Model.Matrix.AlgebraicMatrix;
import GxEngine3D.Model.Plane;
import GxEngine3D.Model.RefPoint3D;
import Shapes.Shape2D.Sqaure;

import java.awt.*;
import java.util.ArrayList;

public class SplittingTest {
    public static void main(String[] args)
    {
        Sqaure sq01 = new Sqaure(Color.white);
        Sqaure sq02 = new Sqaure(Color.white);
        sq02.update();

        AlgebraicMatrix m = new AlgebraicMatrix(2, 4);
        m.addEqautionOfPlane(new Plane(sq01.getShape().get(0)));
        m.addEqautionOfPlane(new Plane(sq02.getShape().get(0)));
        m.gaussJordandElimination();

        m.determineSolution();
        m.getSolutionType();

        ArrayList<double[]> points = new ArrayList<>();
        for (RefPoint3D[] edge:sq02.getEdges())
        {
            //two lines so has to be 4 length
            AlgebraicMatrix edgeIntersect = new AlgebraicMatrix(4, 4);
            edgeIntersect.insertMatrix(m);
            edgeIntersect.addEqautionOfLine(edge[0].toArray(), edge[1].toArray());
            edgeIntersect.gaussJordandElimination();
            edgeIntersect.determineSolution();
            if (edgeIntersect.getSolutionType() == AlgebraicMatrix.SolutionType.POINT)
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
