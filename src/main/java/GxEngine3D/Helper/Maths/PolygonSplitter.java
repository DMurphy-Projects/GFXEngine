package GxEngine3D.Helper.Maths;

public class PolygonSplitter {

    public static double[][] splitPolygonMidPoint(double[][] poly)
    {
        int size = poly.length;
        double[][] newPoly = new double[size+1][];

        double avgX = 0, avgY = 0, avgZ = 0;

        for (int i=0;i<size;i++)
        {
            //copy points across
            newPoly[i] = poly[i];
            //find average point which should be the middle of the polygon
            avgX += newPoly[i][0];
            avgY += newPoly[i][1];
            avgZ += newPoly[i][2];
        }
        newPoly[size] = new double[]{
                avgX / size,
                avgY / size,
                avgZ / size,
        };
        return newPoly;
    }
}
