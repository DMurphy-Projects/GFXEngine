//function declaration

void rasterProject(int *out, double *v0, __global int *size);
bool isOutOfBounds(double *point);
int getTextureAt(int *texturePoint, __global int *textureSize, __global uint *texture);
void applyMatrix(__global double *matrix, double *in, bool _explicit, double *out);
//end function declaration

//tX, tY, tZ: an array of all x/y/z co-ords in the polygon
//tAX, tAY  : an array of all x/y co-ords in the texture anchor
//out:          a pixel grid reduced to 1 dimension
//zMap:         a grid of depth values reduced to 1 dimension

//NOTE: both global sizes should be the same
//TODO: sampling shouldn't be calculated by global sizes, as this creates a max sampling size
__kernel void drawTriangle(
    __global int *screenSize,
    __global uint *texture,
    __global int *textureSize,
    __global double *tX, __global double *tY, __global double *tZ,
    __global double *tAX, __global double *tAY, int noTriangles,
    __global uint *out, __global double *zMap,
    __global double *iMatrix, __global double *eMatrix
    )
{
    double iu = get_local_size(0) * get_group_id(0) + get_local_id(0);
    double iv = get_local_size(1) * get_group_id(1) + get_local_id(1);
    double size = get_global_size(0);

    if (iv + iu < size)
    {
        double u = iu / size;
        double v = iv / size;
        double w = 1 - u - v;

        for (int i=1;i<noTriangles+1;i++)
        {
            int i1 = i; int i2 = i+1;
            double p[3] = {
                (u * tX[0]) + (v * tX[i1]) + (w * tX[i2]),
                (u * tY[0]) + (v * tY[i1]) + (w * tY[i2]),
                (u * tZ[0]) + (v * tZ[i1]) + (w * tZ[i2])
            };
            double p_out[3] = {0, 0, 0};

            applyMatrix(iMatrix, p, false, p_out);//use p, write into p_out
            applyMatrix(eMatrix, p_out, true, p);//use p_out, write into p

            if (!isOutOfBounds(p))
            {
                int raster[2];
                rasterProject(raster, p, screenSize);

                int texturePoint[2] = {
                    (int)(textureSize[0] * ((u * tAX[0]) + (v * tAX[i1]) + (w * tAX[i2]))),
                    (int)(textureSize[1] * ((u * tAY[0]) + (v * tAY[i1]) + (w * tAY[i2])))
                };

                int pos = (raster[1] * screenSize[0]) + raster[0];

                if (zMap[pos] > p[2])
                {
                    zMap[pos] = p[2];

                    int color = getTextureAt(texturePoint, textureSize, texture);
                    out[pos] = color;
                }
            }
        }
    }
}

void applyMatrix(__global double *matrix, double *in, bool _explicit, double *out)
{
    double slice = in[0] * matrix[12] + in[1] * matrix[13] + in[2] * matrix[14] + matrix[15];
    if (_explicit)
    {
        slice = fabs(slice);
    }

    out[0] = in[0] * matrix[0] + in[1] * matrix[1] + in[2] * matrix[2] +  matrix[3];
    out[1] = in[0] * matrix[4] + in[1] * matrix[5] + in[2] * matrix[6] +  matrix[7];
    out[2] = in[0] * matrix[8] + in[1] * matrix[9] + in[2] * matrix[10] +  matrix[11];

    out[0] = out[0] / slice;
    out[1] = out[1] / slice;
    out[2] = out[2] / slice;
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

bool isOutOfBounds(double *point)
{
    return (point[0] > 1 || point[1] > 1 || point[2] > 1 || point[0] < -1 || point[1] < -1|| point[2] < -1);
}

void rasterProject(int *out, double *v0, __global int *size)
{
    out[0] = (int)((v0[0] + 1) * 0.5 * size[0]);
    out[1] = (int) ((1 - (v0[1] + 1) * 0.5) * size[1]);
}