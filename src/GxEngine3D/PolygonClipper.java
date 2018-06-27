package GxEngine3D;

import GxEngine3D.CalculationHelper.VectorCalc;

import java.util.ArrayList;

public class PolygonClipper {

    public double[][] clip(double[][] toCLip)
    {
        double[][] clipped = null;
        //find point that's inside the frustum
        int start = -1;
        for (int i=0;i<toCLip.length;i++)
        {
            if (isInside(toCLip[i]))
            {
                start = i;
                break;
            }
        }
        if (start == -1)
        {
            //they are all outside
            //special handling is needed, for now just cull the polygon from rendering
            clipped =  new double[0][];
        }
        else {
            ArrayList<double[]> clippedPoints = new ArrayList<>();
            int prev = start > 1? start - 1: toCLip.length - 1;

            boolean prevInside = true;
            // loop starting at that point, around back to that point
            for (int i = 0; i <= toCLip.length; i++) {
                int pos = i + start;
                if (pos >= toCLip.length) {
                    pos -= toCLip.length;
                }

                boolean b = isInside(toCLip[pos]);
                if (b ^ prevInside)
                {
                    double[] p;
                    //it transitioned from inside to outside, or vice-versa
                    if (prevInside)
                    {
                        p = interpolateClip(toCLip[pos], toCLip[prev]);
                        clippedPoints.add(p);
                    }
                    else
                    {
                        p = interpolateClip(toCLip[prev], toCLip[pos]);
                        clippedPoints.add(p);
                    }
                    prevInside = b;
                }
                //skip the last one, since we've already added it
                if (b && i < toCLip.length)
                {
                    clippedPoints.add(toCLip[pos]);
                }

                prev = pos;
            }

            clipped = new double[clippedPoints.size()][];
            clippedPoints.toArray(clipped);
        }

        return clipped;
    }

    private boolean isInside(double[] p)
    {
        return  (p[0] >= -1 && p[0] <= 1 && p[1] >= -1 && p[1] <= 1 && p[2] >= -1 && p[2] <= 1);
    }

    //d1 is the one outside
    private double[] interpolateClip(double[] d1, double[] d2)
    {
        d1 = interpolate(d2, d1, 0);
        d1 = interpolate(d2, d1, 1);
        d1 = interpolate(d2, d1, 2);
        return d1;
    }

    private double[] interpolate(double[] lower, double upper[], int index)
    {
        if (upper[index] < -1 || upper[index] > 1)
        {
            double side = upper[index] < -1 ? -1 : 1;
            //vector from inside to outside
            double[] v = VectorCalc.sub(lower, upper);
            double s1 = (lower[index] - side) / v[index];
            double[] v1 = VectorCalc.mul_v_d(v, s1);
            double[] v2 = VectorCalc.sub(lower, v1);
            return v2;
        }
        return upper;
    }
}
