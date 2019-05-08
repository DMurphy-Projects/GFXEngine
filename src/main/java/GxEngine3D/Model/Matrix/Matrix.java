package GxEngine3D.Model.Matrix;

import DebugTools.TextOutput;
import GxEngine3D.Helper.ArrayHelper;

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

    //multiplies v3 with a 4*4 matrix, assumes v3->v4 in[3]=1
    public double[] pointMultiply(double[] in)
    {
        double[] out = new double[4];
        out[0] = in[0] * matrix[0][0] + in[1] * matrix[0][1] + in[2] * matrix[0][2] +  matrix[0][3];
        out[1] = in[0] * matrix[1][0] + in[1] * matrix[1][1] + in[2] * matrix[1][2] +  matrix[1][3];
        out[2] = in[0] * matrix[2][0] + in[1] * matrix[2][1] + in[2] * matrix[2][2] +  matrix[2][3];
        out[3] = in[0] * matrix[3][0] + in[1] * matrix[3][1] + in[2] * matrix[3][2] +  matrix[3][3];
        return out;
    }

    //multiplies v4 with 4*4 matrix
    public double[] pointMultiply4(double[] in)
    {
        double[] out = new double[4];
        out[0] = in[0] * matrix[0][0] + in[1] * matrix[0][1] + in[2] * matrix[0][2] +  in[3] * matrix[0][3];
        out[1] = in[0] * matrix[1][0] + in[1] * matrix[1][1] + in[2] * matrix[1][2] +  in[3] * matrix[1][3];
        out[2] = in[0] * matrix[2][0] + in[1] * matrix[2][1] + in[2] * matrix[2][2] +  in[3] * matrix[2][3];
        out[3] = in[0] * matrix[3][0] + in[1] * matrix[3][1] + in[2] * matrix[3][2] +  in[3] * matrix[3][3];
        return out;
    }

    public double[] pointMultiply3(double[] in)
    {
        double[] out = new double[2];
        out[0] = in[0] * matrix[0][0] + in[1] * matrix[0][1] + matrix[0][2];
        out[1] = in[0] * matrix[1][0] + in[1] * matrix[1][1] + matrix[1][2];
        double w = in[0] * matrix[2][0] + in[1] * matrix[2][1] + matrix[2][2];
        if (w != 1)
        {
            out[0] /= w;
            out[1] /= w;
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

    public double[] flatten()
    {
        return ArrayHelper.flatten(matrix);
    }


    public double[][] inverse_4x4()
    {
        return inverse_4x4(this.matrix);
    }

    //A^-1 = (1 / det(A)) * adj(A)
    public double[][] inverse_4x4(double[][] m)
    {
        double determinant = determinant_4x4(m);
        double[][] adjugate = adjugate_4x4(m);

        for (int i=0;i<4;i++)
        {
            for (int j = 0; j < 4; j++)
            {
                adjugate[i][j] /= determinant;
            }
        }

        return adjugate;
    }

    public double determinant_4x4(double[][] m)
    {
        //uses cofactor expansion along the first column, ie ignoring row and column of the an element to generate a 3x3 matrix. indicated below with 0's
        //-, -, -, -    -, 0, 0, 0      -, 0, 0, 0      -, 0, 0, 0
        //-, 0, 0, 0    -, -, -, -      -, 0, 0, 0      -, 0, 0, 0
        //-, 0, 0, 0    -, 0, 0, 0      -, -, -, -      -, 0, 0, 0
        //-, 0, 0, 0    -, 0, 0, 0      -, 0, 0, 0      -, -, -, -

        //4x4 determinant is sum of (-1)^i * a[i][0] * det(cof_exp(a[i][0])), 0 <= i <= 3
        double determinant = m[0][0] * determinant_3x3(cofactor_expansion_4x4(m, 0, 0))
                           - m[1][0] * determinant_3x3(cofactor_expansion_4x4(m, 1, 0))
                           + m[2][0] * determinant_3x3(cofactor_expansion_4x4(m, 2, 0))
                           - m[3][0] * determinant_3x3(cofactor_expansion_4x4(m, 3, 0));

        return determinant;
    }

    public double[][] cofactor_expansion_4x4(int i, int j)
    {
        return cofactor_expansion_4x4(this.matrix, i, j);
    }

    //i: row
    //j: column
    public double[][] cofactor_expansion_4x4(double[][] m, int i, int j)
    {
        double[][] m_3x3 = new double[3][3];
        int posI = 0, posJ = 0;

        for (int _i=0;_i<4;_i++)
        {
            if (_i == i) continue;
            for (int _j=0;_j<4;_j++)
            {
                if (_j == j) continue;
                m_3x3[posI][posJ] = m[_i][_j];
                posJ++;
            }
            posI++;
            posJ = 0;
        }

        return m_3x3;
    }

    public double determinant_3x3()
    {
        return determinant_3x3(this.matrix);
    }

    public double determinant_3x3(double[][] m)
    {
        double determinant = 0;

        //3 diagonals starting at (0, 0)->(0, 2) moving down/right while wrapping around
        //3 diagonals starting at (0, 2)->(0, 0) moving down/left while wrapping around
        determinant = (m[0][0]*m[1][1]*m[2][2]) + (m[0][1]*m[1][2]*m[2][0]) + (m[0][2]*m[1][0]*m[2][1])
                    - (m[0][2]*m[1][1]*m[2][0]) - (m[0][1]*m[1][0]*m[2][2]) - (m[0][0]*m[1][2]*m[2][1]);

        return determinant;
    }

    //adj(A_ij) = ((-1)^i+j) * det(cof_exp(A, j, i))
    public double[][] adjugate_4x4(double[][] m)
    {
        double[][] adj_4x4 = new double[4][4];

        for (int i=0;i<4;i++)
        {
            for (int j=0;j<4;j++)
            {
                double determinant = determinant_3x3(cofactor_expansion_4x4(m, j, i));
                //((-1)^i+j) is equivalent to scale by -1 when i+j is odd
                if ((i+j) % 2 == 1)
                {
                    determinant *= -1;
                }
                adj_4x4[i][j] = determinant;
            }
        }

        return adj_4x4;
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


    public double[][] getMatrix()
    {
        return matrix;
    }
    public int getRows()
    {
        return m;
    }
}
