package Programs;

import GxEngine3D.Helper.Maths.DistanceCalc;
import GxEngine3D.Helper.Maths.VectorCalc;

public class PointInsideLineTest {
    static double epsilon = 1e-10;
    public static boolean linePointOrientationFast(double[] l0, double[] l1, double[] p)
    {
        double[] v0 = VectorCalc.sub(l0, l1);
        double[] v1 = VectorCalc.sub(l0, p);
        double[] cross = VectorCalc.cross(v0, v1);
        if (Math.abs(cross[0] + cross[1] + cross[2]) > epsilon)
        {
            return false;
        }
        double k_ac = VectorCalc.dot(v0, v1);
        if (k_ac < 0)
        {
            return false;
        }
        else if (k_ac == 0)
        {
            //p shares with l0
            return true;
        }
        double k_ab = VectorCalc.dot(v0, v0);
        if (k_ac > k_ab)
        {
            return false;
        }
        else if (k_ac == k_ab)
        {
            //p shares with l2
            return true;
        }
        return true;
    }
    public static int linePointOrientation(double[] l0, double[] l1, double[] p)
    {
        double lineDist = DistanceCalc.getDistance(l0, l1);
        double distTo0 = DistanceCalc.getDistance(l0, p);
        double distTo1 = DistanceCalc.getDistance(l1, p);

        double total = distTo0 + distTo1;
        if (total > lineDist)
        {
            if (lineDist > distTo0) {
                return -1;
            }
            else
            {
                return 1;
            }
        }
        else
        {
            //is between line segment
            return 0;
        }
    }
    //this is useful as it can tell which ray collisions are on a polygon
    public static void main(String[] arg)
    {
        double epsilon = 1e-10;

        double[] l0 = new double[]{0, 0, 0};
        double[] l1 = new double[]{2, 2, 2};

        double[] m0 = new double[]{1, 1, 1};
        double[] m1 = new double[]{-1, -1, -1};
        double[] m2 = new double[]{3, 3, 3};

        System.out.println(linePointOrientationFast(l0, l1, m0));
        System.out.println(linePointOrientationFast(l0, l1, m1));
        System.out.println(linePointOrientationFast(l0, l1, m2));
    }
}
