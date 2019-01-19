package GxEngine3D.Helper.Iterator;

public interface ITriangleIterator {

    void iterate(double[][] polygon);

    boolean hasNext();

    int[] next();

    double[] get(int index);
}
