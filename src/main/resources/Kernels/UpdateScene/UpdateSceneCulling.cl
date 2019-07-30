
void applyMatrix(__global double *matrix, double *in, double *out);

__kernel void updateScene(
    __global int *screenSize,
    __global double *relativeArray, __global int *polyStartArray,
    __global double *matrix,
    __global double *clipArray, __global double *screenArray,
    __global int *indexArray
)
{
    int n = get_local_size(0) * get_group_id(0) + get_local_id(0);
    bool isOutside = true;
    bool isBehindNear = false;

    for (int i = polyStartArray[n]; i < polyStartArray[n+1]; i=i+3)
    {
        //apply matrix to 3d point
        double in[3] = {
            relativeArray[i], relativeArray[i+1], relativeArray[i+2]
        };
        double out[3];
        applyMatrix(matrix, in, out);

        //copy out into clip array
        for (int ii=0;ii<3;ii++)
        {
            clipArray[i + ii] = out[ii];
        }

        //transform out into screen-space
        screenArray[i] = (out[0] + 1) * 0.5 * screenSize[0];
        screenArray[i+1] = (1 - (out[1] + 1) * 0.5) * screenSize[1];
        screenArray[i+2] = out[2];

        //check:
        //x => everything is outside
        //y => everything is outside
        //z -=> any outside near, all outside far
        if (isOutside)
        {
            if (out[0] > -1 && out[0] < 1 && out[1] > -1 && out[1] < 1 && out[2] < 1)
            {
                isOutside = false;
            }
        }
        if (!isBehindNear)
        {
            if (out[2] < 0)
            {
                isBehindNear = true;
            }
        }
    }

    //when to flag as not being used
    if (isOutside || isBehindNear)
    {
        indexArray[n] = -1;
    }
}

void applyMatrix(__global double *matrix, double *in, double *out)
{
    double slice = in[0] * matrix[12] + in[1] * matrix[13] + in[2] * matrix[14] + matrix[15];
    slice = fabs(slice);

    out[0] = in[0] * matrix[0] + in[1] * matrix[1] + in[2] * matrix[2] +  matrix[3];
    out[1] = in[0] * matrix[4] + in[1] * matrix[5] + in[2] * matrix[6] +  matrix[7];
    out[2] = in[0] * matrix[8] + in[1] * matrix[9] + in[2] * matrix[10] +  matrix[11];

    out[0] = out[0] / slice;
    out[1] = out[1] / slice;
    out[2] = out[2] / slice;
}