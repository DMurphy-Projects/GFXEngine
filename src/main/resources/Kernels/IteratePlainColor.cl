
//triangleArray: contains a list of triangles
//     triangle: contains 3 points
//        point: contains 3 integers
//boundBoxArray: contains a list of elements that make up a bounding box, eg [x, y, width, height]
__kernel void drawTriangle(
    int n,
    __global int *screenSize,
    __global int *triangleArray, __global int *boundBoxArray,
    __global uint *out, __global double *zMap, __global double *planeEqArray, __global int *planeEqArrayInfo
)
{
    int x = get_local_size(0) * get_group_id(0) + get_local_id(0);
    int y = get_local_size(1) * get_group_id(1) + get_local_id(1);

    int color = -1;

    int pos = (y * screenSize[0]) + x;
    //iterate through triangles
    for (int i=0;i<n;i++)
    {
        int bx = boundBoxArray[(i*4)+0];
        int by = boundBoxArray[(i*4)+1];
        int width = boundBoxArray[(i*4)+2];
        int height = boundBoxArray[(i*4)+3];

        //check if inside bounding box
        if (x > bx && x < (bx+width) && y > by && y < (by+height))
        {
            int p1x = triangleArray[(i*6)+0]; int p1y = triangleArray[(i*6)+1];
            int p2x = triangleArray[(i*6)+2]; int p2y = triangleArray[(i*6)+3];
            int p3x = triangleArray[(i*6)+4]; int p3y = triangleArray[(i*6)+5];

            double denominator = ((p2y - p3y)*(p1x - p3x) + (p3x - p2x)*(p1y - p3y));
            double v = ((p2y - p3y)*(x - p3x) + (p3x - p2x)*(y - p3y)) / denominator;
            double w = ((p3y - p1y)*(x - p3x) + (p1x - p3x)*(y - p3y)) / denominator;

            if ((w >= 0) && (v >= 0) && (w + v < 1))
            {
                //we need to find z value, by using plane eq. ax + by +cz + d = 0, we can find (a, b, c, d) using triangle points
                //then substitute (x, y) and re-arrange for z, thus
                //z = (-ax - by - d) / c
                int ii = planeEqArrayInfo[i];//controls which plane eqaution we are using
                double planeEqa = planeEqArray[(ii*4)+0];
                double planeEqb = planeEqArray[(ii*4)+1];
                double planeEqc = planeEqArray[(ii*4)+2];
                double planeEqd = planeEqArray[(ii*4)+3];

                double z = (-(planeEqa*x) - (planeEqb*y) - planeEqd) / planeEqc;
                if (z < 1 && z > 0 && zMap[pos] > z)
                {
                    zMap[pos] = z;
                    color = 1000;//TODO
                }
            }
        }
    }

    if (color > 0)
    {
        out[pos] = color;
    }
}