package GxEngine3D.Model.Matrix;

import DebugTools.TextOutput;

public class Matrix {

    double[][] matrix;
    int m, n, curM = 0;

    public Matrix(int m, int n)
    {
        matrix = new double[m][n];
        this.m = m;
        this.n = n;
    }
    public Matrix(double[][] m)
    {
        matrix = m;
        this.m = m.length;
        this.n = m[0].length;
    }

    public void insertMatrix(double[][] m)
    {
        //compatibility checks
        if (m[0].length != this.n) return;
        if (m.length<= curM) return;

        for (double[] e:m)
        {
            addEqaution(e);
        }
    }

    public void insertMatrix(Matrix m)
    {
        insertMatrix(m.matrix);
    }

    public double[] pointMultiply(double[] in)
    {
        double[] out = new double[3];
        out[0] = in[0] * matrix[0][0] + in[1] * matrix[0][1] + in[2] * matrix[0][2] + /* in.z = 1 */ matrix[0][3];
        out[1] = in[0] * matrix[1][0] + in[1] * matrix[1][1] + in[2] * matrix[1][2] + /* in.z = 1 */ matrix[1][3];
        out[2] = in[0] * matrix[2][0] + in[1] * matrix[2][1] + in[2] * matrix[2][2] + /* in.z = 1 */ matrix[2][3];
        double w = in[0] * matrix[3][0] + in[1] * matrix[3][1] + in[2] * matrix[3][2] + /* in.z = 1 */ matrix[3][3];

        // normalize if w is different than 1 (convert from homogeneous to Cartesian coordinates)
        if (w != 1) {
            out[0] /= w;
            out[1] /= w;
            out[2] /= w;
        }
        return out;
    }

    public double[][] matrixMultiply(Matrix m)
    {
        return matrixMultiply(m.matrix);
    }

    public double[][] matrixMultiply(double[][] in)
    {
        if (this.m == in[0].length)
        {
            double[][] result = new double[this.m][in[0].length];
            for(int i = 0; i < this.m; ++i) {
                for (int j = 0; j < in[0].length; ++j) {
                    for (int k = 0; k < this.n; ++k) {
                        result[i][j] += matrix[i][k] * in[k][j];
                    }
                }
            }
            return result;
        }
        TextOutput.println("Cannot Multiply Matrices: Matrix01 length(" + this.n+") does not eqaul Matrix02 height("+in[0].length +")");
        return null;
    }

    public void addEqaution(double[] e)
    {
        if (e.length > n)
        {
            TextOutput.println("Eqaution in wrong format for this "+m+" by "+n+" matrix", 0);
        }
        else
        {
            if (curM > m) {
                TextOutput.println("Exceeds matrix length", 0);
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
        TextOutput.println("Scale", 1);
        TextOutput.println(this, 2);
        return newRow;
    }

    public double[] scale(int m, double[] v)
    {
        double[] newRow = new double[this.n];
        for (int i=0;i<n;i++)
        {
            newRow[i] = matrix[m][i] * v[i];
        }
        TextOutput.println("Multiply Matrix", 1);
        TextOutput.println(this, 2);
        return newRow;
    }

    public double[] add(int m, double v)
    {
        double[] newRow = new double[this.n];
        for (int i=0;i<n;i++)
        {
            newRow[i] = matrix[m][i] + v;
        }
        TextOutput.println("Add Constant", 1);
        TextOutput.println(this, 2);
        return newRow;
    }

    public double[] add(int m, double[] _matrix)
    {
        double[] newRow = new double[this.n];
        for (int i=0;i<n;i++)
        {
            newRow[i] = matrix[m][i] + _matrix[i];
        }
        TextOutput.println("Add AlgebraicMatrix", 1);
        TextOutput.println(this, 2);
        return newRow;
    }

    public double[][] addMatrix(double[][] m)
    {
        double[][] newMatrix = new double[this.m][n];
        for (int i=0;i<this.m;i++)
        {
            newMatrix[i] = add(i, m[i]);
        }
        return newMatrix;
    }

    public double[][] scaleMatrix(double[][] m)
    {
        double[][] newMatrix = new double[this.m][n];
        for (int i=0;i<this.m;i++)
        {
            newMatrix[i] = scale(i, m[i]);
        }
        return newMatrix;
    }

    @Override
    public String toString() {
        String s = "\n";
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

    public int getRows()
    {
        return m;
    }
}
