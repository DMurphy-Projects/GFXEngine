package GxEngine3D.Helper.Iterator;

public class RegularTriangleIterator extends BaseIterator {

    int pos;

    @Override
    public void iterate(double[][] polygon) {
        super.iterate(polygon);
        pos = 1;
    }

    @Override
    public boolean hasNext() {
        return pos < polygon.length-1;
    }

    @Override
    public int[] next() {
        int[] index = new int[]{0, pos, pos+1};
        pos++;
        return index;
    }
}
