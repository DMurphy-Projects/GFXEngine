package GxEngine3D.Helper.Iterator;

public interface ITriangleIterator {

    void iterate(double[][] polygon);

    boolean hasNext();

    double[][] next();
    int[] nextIndices();

    double[] get(int index);
}
