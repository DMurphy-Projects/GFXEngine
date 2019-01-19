package GxEngine3D.Helper.Iterator;

public class MidPointIterator extends BaseIterator{

    int pos, prev;

    @Override
    public void iterate(double[][] polygon) {

        int size = polygon.length;
        double[][] newPoly = new double[size+1][];

        double avgX = 0, avgY = 0, avgZ = 0;

        for (int i=0;i<size;i++)
        {
            //copy points across
            newPoly[i] = polygon[i];

            avgX += polygon[i][0];
            avgY += polygon[i][1];
            avgZ += polygon[i][2];
        }

        newPoly[size] = new double[]{
                avgX / size,
                avgY / size,
                avgZ / size
        };

        pos = 0;
        prev = polygon.length - 1;

        super.iterate(newPoly);
    }

    @Override
    public boolean hasNext() {
        return pos < polygon.length-1;
    }

    @Override
    public double[][] next() {
        int[] index = nextIndices();
        prev = pos;
        pos++;
        return new double[][] {
                polygon[index[0]],
                polygon[index[1]],
                polygon[index[2]]
        };
    }

    @Override
    public int[] nextIndices() {
        return new int[]{prev, pos, polygon.length-1};
    }
}
