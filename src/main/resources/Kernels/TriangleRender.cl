//this is an implementation for drawing a triangle with a solid color

//function declaration

bool isOutOfBounds(double x, double y, __global int *screenSize);

//function end

//note:
//noPolygons:   the number of vertices in the polygon
//tX, tY, tZ:   an array of all x/y/z co-ords in the polygon in screen-space
//out:          a pixel grid reduced to 1 dimension
//zMap:         a grid of depth values reduced to 1 dimension
//planeEq:      [a, b, c, d] of the form, ax + by + cz + d = 0
//offset;       this kernel iterates over a rectangle region of the screen, this value moves that region
__kernel void drawTriangle(
    __global int *screenSize,
    int color, int noPolygons,
    __global double *tX, __global double *tY, __global double *tZ,
    __global uint *out, __global double *zMap,
    __global double *planeEq, __global int *offset
    )
{
    double x = (get_local_size(0) * get_group_id(0) + get_local_id(0)) + offset[0];
    double y = (get_local_size(1) * get_group_id(1) + get_local_id(1)) + offset[1];

    if (!isOutOfBounds(x, y, screenSize))
    {
        //http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
        bool inside = false;
        for ( int i = 0, j = noPolygons - 1 ; i < noPolygons ; j = i++ )
        {
            if ( ( tY[i] > y ) != ( tY[j] > y ) &&
                x < ( tX[j] - tX[i] ) * ( y - tY[i] ) / ( tY[j] - tY[i] ) + tX[i] )
            {
                inside = !inside;
            }
        }
        if (inside)
        {
            int pos = (y * screenSize[0]) + x;

            //we need to find z value, by using plane eq. ax + by +cz + d = 0, we can find (a, b, c, d) using triangle points
            //then substitute (x, y) and re-arrange for z, thus
            //z = (-ax - by - d) / c
            double z = (-(planeEq[0]*x) - (planeEq[1]*y) - planeEq[3]) / planeEq[2];
            if (zMap[pos] > z)
            {
                zMap[pos] = z;
                out[pos] = color;
            }
        }
    }
}

bool isOutOfBounds(double x, double y, __global int *screenSize)
{
    return (x > screenSize[0]-1 || y > screenSize[1]-1 || x < 0 || y < 0);
}