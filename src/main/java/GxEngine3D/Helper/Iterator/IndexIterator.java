package GxEngine3D.Helper.Iterator;

public class IndexIterator extends BaseIterator{

    int pos, size;

    //implementation to give only indices but will work with any type of array
    public void iterate(int size)
    {
        this.size = size;
        this.pos = 1;
    }

    @Override
    public boolean hasNext() {
        return pos < size-1;
    }

    @Override
    public int[] next() {
        int[] index = new int[]{0, pos, pos+1};
        pos++;
        return index;
    }
}
