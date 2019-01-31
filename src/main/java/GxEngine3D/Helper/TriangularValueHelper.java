package GxEngine3D.Helper;

public class TriangularValueHelper {

    public static int[] findClosestMultiPair(int n, int max)
    {
        //t(n) = x.y, where x is closest to max and x.y is closest to t(n) and is triangular
        int triangle = getTriangleValue(n);

        //if we're under the cap, don't do anything
        if (triangle <= max || max <= 1)
        {
            return new int[]{triangle, 1};
        }

        System.out.println("Target: "+triangle);

        int[] xy = new int[2];
        int bestScore = Integer.MAX_VALUE;

        for (int i=triangle;i>=1;i--)
        {
            for (int ii=max;ii>=1;ii--)
            {
                //the ideal is when both of these sum to 0
                int difX = max - ii; int difY = triangle - (i*ii);
                if (difY < 0) continue;//new target must be less than existing target
                if (!isTriangular(i*ii)) continue;//new target must be triangular in order to draw an entire triangle
                //is t*ii triangular?
                if (bestScore > difX+difY)
                {
                    bestScore = difX+difY;
                    xy[0] = i*ii; xy[1] = ii;
                    System.out.println("Found: "+xy[0]+" "+xy[1]);
                }
            }
        }
        return xy;
    }

    public static boolean isTriangular(int tValue)
    {
        return getTriangleValue(guessTriangle(tValue)) == tValue;
    }

    public static int guessTriangle(int tValue)
    {
        return (int) Math.round(Math.sqrt(tValue*2));
    }

    public static int getTriangleValue(int n)
    {
        return (n * (n+1))/2;
    }
}
