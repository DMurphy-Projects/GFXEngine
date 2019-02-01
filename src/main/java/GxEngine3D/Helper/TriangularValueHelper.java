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
            for (int ii=1;ii<=max;ii++)
            {
                //the ideal is when both of these sum to 0
                int distFromTarget = triangle - (i*ii);
                if (distFromTarget < 0) break;//new target must be less than existing target
                if (!isTriangular(i*ii)) continue;//new target must be triangular in order to draw an entire triangle

                int distFromMax = max - ii;
                int score = distFromMax + distFromTarget;
                if (bestScore > score)
                {
                    bestScore = score;
                    xy[0] = i*ii; xy[1] = ii;
                    System.out.println("Found: "+xy[0]+" "+xy[1]+" "+score);
                }
            }
        }
        System.out.println("Used: "+xy[0]+" "+xy[1]);
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
