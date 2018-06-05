package Programs;

import GxEngine3D.CalculationHelper.Matrix;
import GxEngine3D.CalculationHelper.VectorCalc;
import GxEngine3D.Model.Plane;
import GxEngine3D.Model.Vector;
import Shapes.Shape2D.Sqaure;

import java.awt.*;

public class MatrixTest {

    public static void main(String[] args)
    {
        Sqaure sq01 = new Sqaure(0, 0, 2, 5, Color.white);
        //plane based from a square
        Plane plane = new Plane(sq01.getShape().get(0));
        double[] nV = plane.getNV().toArray();
        double[] p0 = plane.getP();
        double[] planeEq0 = VectorCalc.plane_v3_pointForm(nV, p0);

        double[] p1 = new double[]{1, 1, -10};
        double[] p2 = new double[]{1, 1, 10};

        VectorCalc.p3_in_line_seg(p1, p2, p1);
        VectorCalc.p3_in_line_seg(p1, p2, p2);

        double[] v1 = VectorCalc.sub(p1, p2);

        double[] noise01 = new double[]{1, 1, 0};
        double[] noise02 = new double[]{0, 1, 1};
        double[] noise03 = new double[]{1, 0, 1};
        if (VectorCalc.v_v_equals(v1, noise01))
        {
            System.out.println("v1 same as n1");
            noise01 = noise03;
        }
        if(VectorCalc.v_v_equals(v1, noise02))
        {
            System.out.println("v1 same as n2");
            noise02 = noise03;
        }
        //we need to find two planes where their intersection is the line vector that we want
        //to do this we setup a plane with the vector + a random/arbitrary vector
        //the random vector needs to be different and not parallel in both planes
        //in short the difference between numbers cannot be one of the numbers, ie 1 and 2 => 2-1=1
        Plane plane1 = new Plane(new Vector(v1), new Vector(VectorCalc.add(v1, noise01)), p1);
        Plane plane2 = new Plane(new Vector(v1), new Vector(VectorCalc.add(v1, noise02)), p1);

        double[] planeEq1 = VectorCalc.plane_v3_pointForm(plane1.getNV().toArray(), plane1.getP());
        double[] planeEq2 = VectorCalc.plane_v3_pointForm(plane2.getNV().toArray(), plane2.getP());

        Matrix m = new Matrix(2, 4);
        m.addEqaution(planeEq1);
        m.addEqaution(planeEq2);


        System.out.println(m);
        m.gaussJordandElimination();
        System.out.println(m);
        m.determineSolution();
        if (m.getSolutionType() == Matrix.SolutionType.LINE)
        {
            if (m.satisfiesEquation(p1))
            {
                System.out.println("True");
            }
            else
            {
                System.out.println("False");
            }
        }
    }
}
