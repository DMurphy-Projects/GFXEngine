package GxEngine3D.Helper;

public class PolygonClipBoundsChecker {

    private static int X=0, Y=1, DEPTH=2;

    //this is an all or nothing kind of cull
    //if a polygon is partially on screen we will attempt to draw it
    //NOTE: this is a simplistic implementation and will attempt to draw edge cases that are clearly outside the frustum
    public static boolean shouldCull(double[][] polygon) {

        if (checkLowest(polygon, X, 1)) return true;
        if (checkHighest(polygon, X, -1)) return true;

        if (checkLowest(polygon, Y, 1)) return true;
        if (checkHighest(polygon, Y, -1)) return true;

        if (checkLowest(polygon, DEPTH, 1)) return true;
        if (checkNearPlaneDepth(polygon)) return true;

        return false;
    }

    private static boolean checkNearPlaneDepth(double[][] polygon)
    {
        for (double[] point: polygon)
        {
            //if any are outside the near plane, we should cull
            if (point[2] < -1)
            {
                return true;
            }
        }
        return false;
    }

    //assumes the polygon is on screen
    public static boolean intersectsNearPlane(double[][] polygon)
    {
        for (int i=0;i<polygon.length;i++)
        {
            if (polygon[i][2] < 0)
            {
                return true;
            }
        }
        return false;
    }

    private static boolean checkLowest(double[][] polygon, int index, int value)
    {
        for (double[] point: polygon)
        {
            //we're looking for the first point that means its not out of bounds
            if (point[index] < value)
            {
                return false;
            }
        }
        return true;
    }

    private static boolean checkHighest(double[][] polygon, int index, int value)
    {
        for (double[] point: polygon)
        {
            if (point[index] > value)
            {
                return false;
            }
        }
        return true;
    }
}
