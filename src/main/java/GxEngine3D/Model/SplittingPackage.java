package GxEngine3D.Model;

public class SplittingPackage {
    double[] point;
    int index;
    public SplittingPackage(double[] p, int i)
    {
        point = p;
        index = i;
    }

    public double[] getPoint() {
        return point;
    }

    public int getIndex() {
        return index;
    }
}
