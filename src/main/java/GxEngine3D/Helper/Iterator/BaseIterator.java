package GxEngine3D.Helper.Iterator;

public abstract class BaseIterator implements ITriangleIterator{

    double[][] polygon;

    public void iterate(double[][] polygon)
    {
        this.polygon = polygon;
    }

    public abstract boolean hasNext();
    public abstract int[] next();

    public double[] get(int index)
    {
        return polygon[index];
    }
}
