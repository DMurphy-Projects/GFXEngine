#ifdef INFINITY
/* INFINITY is supported */
#endif

//function declaration
double len(double *v0);

void rasterProject(int *out, double *v0, __global int *size);
bool isOutOfBounds(int *raster, __global int *size);
int getTextureAt(int *texturePoint, __global int *textureSize, __global uint *texture);
//end function declaration


//NOTE: single dimension kernel is about twice as fast as single thread cpu
//    : single dimension kernel is about the same as the single dimension thread pool
//    : 2 dimension thread pool had trouble handling that many runnables, so it stands to reason that a 2 dim kernel would be the highest performance
//      however due to the nature of the problem dynamic parallelism would have to play a role
//      or do a 2 pass alg that would use a kernel to calculate the width at each point
//TODO: create cuda version as it has easier/possible dynamic parallelism, jocl may not have dp support
__kernel void drawTriangle(
    __global uint *output,
    __global int *screenSize,
    __global uint *texture,
    __global int *textureSize,
    __global double *textureOrigin,
    __global double *textureV1, __global double *textureV2,
    __global double *origin,
    __global double *travel, __global double *otherEnd,
    double height,
    double direction, __global double *debug
    )
{
    //if travel is the vector we will start horizontal lines at then other is the vector where horizontal lines end
    double other[3] = {
        origin[0] - otherEnd[0],
        origin[1] - otherEnd[1],
        origin[2] - otherEnd[2]
        };//should this just be passed in?

    unsigned int i = get_global_id(0);

    double _i = i / height;
    double p0[3] = {
        p0[0] = origin[0] - (travel[0] * _i),
        p0[1] = origin[1] - (travel[1] * _i),
        p0[2] = origin[2] - (travel[2] * _i)
    };

    //must find point on line where y=p0[1]
    //y = mx + c
    //v01 => (x2-x1, y2-y1)
    double m = other[1] / other[0];
    double x;
    if (m == INFINITY || m == -INFINITY)
    {
        x = origin[0];
    }
    else
    {
        //y1 = m(x1) + c => c = y1 - m(x1)
        double c = origin[1] - (m * origin[0]);
        //mx = y - c => x = (y - c) / m
        x = (p0[1] - c) / m;
    }

    double width = fabs(screenSize[0] * (x - p0[0]) / 2);

    for (double ii=0; ii < width; ii += 1) {
        //this is the horizontal relative co-ord, not the relative co-ord of the other vector that makes up the triangle
        double _ii = ii / (screenSize[0] / 2);

        double moveVector[3] = {direction>0?_ii : -_ii, 0, 0};
        double p1[3] = {
            p0[0] + moveVector[0],
            p0[1] + moveVector[1],
            p0[2] + moveVector[2],
        };
        int raster[2];
        rasterProject(raster, p1, screenSize);
        if (isOutOfBounds(raster, screenSize)) continue;

        double test = 1 - (ii / width);

        double travelMagnitude[3] = {
            travel[0] * -_i,
            travel[1] * -_i,
            travel[2] * -_i,
        };
        //this is the position on the other side

        double p2[3] = {
             p1[0] - (travelMagnitude[0] * test),
             p1[1] - (travelMagnitude[1] * test),
             p1[2] - (travelMagnitude[2] * test),
         };
        double pv01[3] = {
            p2[0] - origin[0],
            p2[1] - origin[1],
            p2[2] - origin[2],
        };
        double otherVector[3] = {
            otherEnd[0] - origin[0],
            otherEnd[1] - origin[1],
            otherEnd[2] - origin[2],
        };
        double newII = len(pv01) / len(otherVector);

        double pv02[3] = {
           (p1[0] - pv01[0]) - origin[0],
           (p1[1] - pv01[1]) - origin[1],
           (p1[2] - pv01[2]) - origin[2]
        };

        double travelLength = sqrt((travel[0]*travel[0]) + (travel[1]*travel[1]) + (travel[2]*travel[2]));

        double newI = len(pv02) / travelLength;

        double textureV1Magnitude[2] = {
            textureV1[0] * newI,
            textureV1[1] * newI
        };
        double t0[2] = {
            textureOrigin[0] - textureV1Magnitude[0],
            textureOrigin[1] - textureV1Magnitude[1]
        };


        double textureV2Magnitude[2] = {
            textureV2[0] * newII,
            textureV2[1] * newII
        };

        double t1[2] = {
            t0[0] - textureV2Magnitude[0],
            t0[1] - textureV2Magnitude[1],
        };
        int texturePoint[2] = {
                (int) (t1[0] * textureSize[0]),
                (int) (t1[1] * textureSize[1])
        };
        int color = getTextureAt(texturePoint, textureSize, texture);

        output[(raster[1] * screenSize[0]) + raster[0]] = color;
    }
}

int getTextureAt(int *texturePoint, __global int *textureSize, __global uint *texture)
{
    if (texturePoint[0] < 0 || texturePoint[1] < 0 || texturePoint[0] >= textureSize[0] || texturePoint[1] >= textureSize[1])
    {
        return (255 << 16) | (255 << 8 ) | 255;
    }
    int index = (texturePoint[1] * textureSize[0]) + texturePoint[0];
    return texture[index];
}

bool isOutOfBounds(int *raster, __global int *size)
{
    return (raster[0] >= size[0] || raster[1] >= size[1] || raster[0] < 0 || raster[1] < 0);
}

void rasterProject(int *out, double *v0, __global int *size)
{
    out[0] = (int)((v0[0] + 1) * 0.5 * size[0]);
    out[1] = (int) ((1 - (v0[1] + 1) * 0.5) * size[1]);
}

double len(double *v0)
{
    double d = 0;
    for (int i=0;i<3;i++)
    {
        d += v0[i] * v0[i];
    }
    return sqrt(d);
}