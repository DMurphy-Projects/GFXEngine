
//n: total number of polygons
//polygonArray: contains a list of polygons
//     polygon: contains a list of 3d points
//       point: contains 3 double values
//polygonStartArray: contains memory offset information to separate polygons in the main array
//boundsBoxArray: contains a list of elements that make up a bounding box, eg [x, y, width, height]
//                except that its a box around the polygon instead of individual triangles
//colorArray: color information for each polygon
//indexArray: contains a list of indices to allow for skipping of polygons not inside the screen bounds

//calculates two maps, the out-map used to determine which triangle is rendered on which pixel and the z-map
//used to determine order
__kernel void calculateMapInfo(
    int n,
    __global int *screenSize,
    __global double *polygonArray, __global int *polygonStartArray,
    __global int *boundBoxArray,
    __global int *out, __global double *zMap,
    __global int *indexArray
)
{
    int x = get_local_size(0) * get_group_id(0) + get_local_id(0);
    int y = get_local_size(1) * get_group_id(1) + get_local_id(1);
    int pos = (y * screenSize[0]) + x;

    //set starting values
    out[(pos * 2) + 0] = -1;
    out[(pos * 2) + 1] = -1;

    //iterate over polygons
    for (int _i=0;_i<n;_i++)
    {
        int i = indexArray[_i];

        int bx = boundBoxArray[(_i*4)+0];
        int by = boundBoxArray[(_i*4)+1];
        int width = boundBoxArray[(_i*4)+2];
        int height = boundBoxArray[(_i*4)+3];

        if (x > bx && x < (bx+width) && y > by && y < (by+height))
        {
            //since polygonStartArray is a memory offset array, and there are 3 doubles per point, size = length / 3;
            //triangle count follows n-2, where n is point count
            int size = ((polygonStartArray[i+1] - polygonStartArray[i]) / 3) - 2;

            //polygonOffset + pointOffset + componentOffset
            int polygonOffset = polygonStartArray[i];
            double p1x = polygonArray[polygonOffset + 0]; double p1y = polygonArray[polygonOffset + 1];
            //iterate over triangles in the polygon, we want to start at 1 for iterating reasons, so offset by 1

            for (int ii=1;ii<size+1;ii++)
            {
                double p2x = polygonArray[polygonOffset + (ii*3) + 0];     double p2y = polygonArray[polygonOffset + (ii*3) + 1];
                double p3x = polygonArray[polygonOffset + ((ii+1)*3) + 0]; double p3y = polygonArray[polygonOffset + ((ii+1)*3) + 1];

                double denominator = (p2y - p3y)*(p1x - p3x) + (p3x - p2x)*(p1y - p3y);
                double v = ((p2y - p3y)*(x - p3x) + (p3x - p2x)*(y - p3y)) / denominator;
                double w = ((p3y - p1y)*(x - p3x) + (p1x - p3x)*(y - p3y)) / denominator;
                double u = 1 - v - w;

                if ((w >= 0) && (v >= 0) && (u >= 0))
                {
                    double p1z = polygonArray[polygonOffset + 2];
                    double p2z = polygonArray[polygonOffset + (ii*3) + 2];
                    double p3z = polygonArray[polygonOffset + ((ii+1)*3) + 2];

                    double z = (v*p1z) + (w*p2z) + (u*p3z);
                    if (z < 1 && z > 0 && zMap[pos] > z)
                    {
                        zMap[pos] = z;

                        out[(pos * 2) + 0] = _i;
                        out[(pos * 2) + 1] = ii;
                    }
                }
            }
        }
    }
}