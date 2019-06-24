
//triangleArray: contains a list of triangles
//     triangle: contains 3 points
//        point: contains 3 integers
//boundBoxArray: contains a list of elements that make up a bounding box, eg [x, y, width, height]
__kernel void drawTriangle(
    int n,
    __global int *screenSize,
    __global double *triangleArray, __global int *boundBoxArray,
    __global uint *out, __global double *zMap,
    __global uint *colorArray
)
{
    int x = get_local_size(0) * get_group_id(0) + get_local_id(0);
    int y = get_local_size(1) * get_group_id(1) + get_local_id(1);

    int color = 0;
    bool draw = false;

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
            double p1x = triangleArray[(i*9)+0]; double p1y = triangleArray[(i*9)+1]; double p1z = triangleArray[(i*9)+2];
            double p2x = triangleArray[(i*9)+3]; double p2y = triangleArray[(i*9)+4]; double p2z = triangleArray[(i*9)+5];
            double p3x = triangleArray[(i*9)+6]; double p3y = triangleArray[(i*9)+7]; double p3z = triangleArray[(i*9)+8];

            double denominator = (p2y - p3y)*(p1x - p3x) + (p3x - p2x)*(p1y - p3y);
            double v = ((p2y - p3y)*(x - p3x) + (p3x - p2x)*(y - p3y)) / denominator;
            double w = ((p3y - p1y)*(x - p3x) + (p1x - p3x)*(y - p3y)) / denominator;
            double u = 1 - v - w;

            if ((w >= 0) && (v >= 0) && (u >= 0))
            {
                double z = (v*p1z) + (w*p2z) + (u*p3z);
                if (z < 1 && z > 0 && zMap[pos] > z)
                {
                    zMap[pos] = z;
                    color = colorArray[i];
                    draw = true;
                }
            }
        }
    }

    if (draw)
    {
        out[pos] = color;
    }
}