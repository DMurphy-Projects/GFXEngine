package GxEngine3D.CalculationHelper;

import GxEngine3D.Model.Plane;
import GxEngine3D.Model.Vector;

import java.util.ArrayList;

public class Matrix {

    double[][] matrix;
    int m, n, curM = 0;
    double epsilon = 1e-10;
    boolean debug = false;

    public enum SolutionType
    {
        UNDEFINED,
        UNSOLVABLE,
        POINT,
        LINE,
        PLANE
    }

    SolutionType solutionType = SolutionType.UNDEFINED;

    public Matrix(int m, int n)
    {
        matrix = new double[m][n];
        this.m = m;
        this.n = n;
    }

    //a plane counts as 1 but a line counts as 2
    public void addEqautionOfLine(double[] p1, double[] p2)
    {
        double[] v1 = VectorCalc.sub_v3v3(p1, p2);
        double[] noise01 = new double[]{1, 1, 0};
        double[] noise02 = new double[]{0, 1, 1};
        double[] noise03 = new double[]{1, 0, 1};
        if (VectorCalc.v3_v3_eqauls(v1, noise01))
        {
            if (debug) {
                System.out.println("v1 same as n1");
            }
            noise01 = noise03;
        }
        else if(VectorCalc.v3_v3_eqauls(v1, noise02))
        {
            if (debug) {
                System.out.println("v1 same as n2");
            }
            noise02 = noise03;
        }
        Plane plane1 = new Plane(new Vector(v1), new Vector(VectorCalc.add_v3v3(v1, noise01)), p1);
        Plane plane2 = new Plane(new Vector(v1), new Vector(VectorCalc.add_v3v3(v1, noise02)), p1);
        double[] planeEq1 = VectorCalc.plane_v3_pointForm(plane1.getNV().toArray(), plane1.getP());
        double[] planeEq2 = VectorCalc.plane_v3_pointForm(plane2.getNV().toArray(), plane2.getP());
        addEqaution(planeEq1);
        addEqaution(planeEq2);
    }

    public void addEqautionOfPlane(Plane plane)
    {
        double[] eq = VectorCalc.plane_v3_pointForm(plane.getNV().toArray(), plane.getP());
        addEqaution(eq);
    }

    public void addEqaution(double[] e)
    {
        if (e.length > n)
        {
            System.out.println("Eqaution in wrong format for this "+m+" by "+n+" matrix");
        }
        else
        {
            if (curM > m) {
                System.out.println("Exceeds matrix length");
            }
            else
            {
                matrix[curM] = e;
                curM++;
            }
        }
    }

    public double[] scale(int m, double v)
    {
        double[] newRow = new double[this.n];
        for (int i=0;i<n;i++)
        {
            newRow[i] = matrix[m][i] * v;
        }
        if (debug) {
            System.out.println("Scale");
            System.out.println(this);
        }
        return newRow;
    }

    public double[] add(int m, double v)
    {
        double[] newRow = new double[this.n];
        for (int i=0;i<n;i++)
        {
            newRow[i] = matrix[m][i] + v;
        }
        if (debug) {
            System.out.println("Add");
            System.out.println(this);
        }
        return newRow;
    }

    public double[] add(int m, double[] _matrix)
    {
        double[] newRow = new double[this.n];
        for (int i=0;i<n;i++)
        {
            newRow[i] = matrix[m][i] + _matrix[i];
        }
        return newRow;
    }

    private void swapOrdering()
    {
        //pre swapping to ensure that there is a non 0 value on the diagonal
        int col = 0, start = 0;
        do {
            for (int i = start; i < m; i++) {
                if (matrix[i][col] != 0) {
                    if (start != i) {
                        double[] temp = matrix[i];
                        matrix[i] = matrix[start];
                        matrix[start] = temp;
                    }
                    start++;
                    break;
                }
            }
            col++;
        }while(col < m && col < n);
        if (debug) {
            System.out.println("Swap");
            System.out.println(this);
        }
    }

    public void gaussianElimination()
    {
        if (debug)
        {
            System.out.println("Gaussian Elimination");
            System.out.println(this);
        }
        //this may need to happen after every pivot
        swapOrdering();
        for (int i=0;i<m;i++)
        {
            for (int ii=0;ii<n;ii++)
            {
                //find leading edge
                if (matrix[i][ii] == 0) continue;
                if (matrix[i][ii] == 1) break;
                //turn it to a 1
                double scale = 1d / matrix[i][ii];
                matrix[i] = scale(i, scale);
                matrix[i][ii] = 1;//to avoid double imprecision
                //turn it to a 0
                for (int iii=i+1;iii<m;iii++)
                {
                    if (matrix[iii][ii] == 0) continue;
                    matrix[iii] = add(iii,  scale(i, -matrix[iii][ii]));
                    matrix[iii][ii] = 0;
                }
                break;
            }
        }
    }

    private void cullSmallValues()
    {
        for (int i=0;i<m;i++) {
            for (int ii = 0; ii < n; ii++) {
                if (Math.abs(matrix[i][ii]) < epsilon)
                {
                    matrix[i][ii] = 0;
                }
            }
        }
    }

    public void gaussJordandElimination()
    {
        gaussianElimination();
        swapOrdering();
        for (int i=m-1;i>=0;i--)
        {
            for (int ii=0;ii<n;ii++) {
                //find leading edge, we know its in gaussian form so any non 0 value is 1
                if (matrix[i][ii] == 0) continue;
                for (int iii=i-1;iii>=0;iii--)
                {
                    if (iii < 0) break;
                    matrix[iii] = add(iii, scale(i, -matrix[iii][ii]));
                }
                matrix[i][ii] = 1;//double precision sometimes leads to 0.99999999 when it should be 1
                break;
            }
        }
        cullSmallValues();
        if (debug)
        {
            System.out.println("GJ Cull");
            System.out.println(this);
        }
    }

    public void determineSolution()
    {
        //if every row has only 1 non 0 value, then is point
        boolean isPoint = true, unsolvable = false;
        for (int i=0;i<m;i++) {
            int count = 0;
            for (int ii = 0; ii < n-1; ii++) {
                if (matrix[i][ii] != 0)
                {
                    count++;
                }
            }
            if (count > 1)
            {
                isPoint = false;
                break;
            }
            if (count == 0 && matrix[i][n-1] != 0)
            {
                //if all elements are 0 but the last column isnt then the matrix is unsolvable
                //ie 0x + 0y + 0z = 5, cant be done
                unsolvable = true;
                break;
            }
        }
        //TODO other types
        //what makes a line vs a plane, can an area be represented here?
        if (unsolvable)
        {
            solutionType = SolutionType.UNSOLVABLE;
        }
        else if (isPoint)
        {
            solutionType = SolutionType.POINT;
        }
    }


    public SolutionType getSolutionType() {
        return solutionType;
    }

    public Double[] getPointSolution()
    {
        if (solutionType == SolutionType.POINT)
        {
            ArrayList<Double> point = new ArrayList<>();
            for (int i=0;i<m;i++) {
                for (int ii = 0; ii < n - 1; ii++) {
                    if (matrix[i][ii] == 1)
                    {
                        point.add(i, matrix[i][n-1]);
                    }
                }
            }
            Double[] p = new Double[point.size()];
            point.toArray(p);
            return p;
        }
        return null;
    }

    @Override
    public String toString() {
        String s = "";
        for (int i=0;i<m;i++)
        {
            for (int ii=0;ii<n;ii++)
            {
                s += matrix[i][ii] +" ";
            }
            s += "\n";
        }
        return s;
    }

    public void seteDebug(boolean d)
    {
        debug = d;
    }
}
