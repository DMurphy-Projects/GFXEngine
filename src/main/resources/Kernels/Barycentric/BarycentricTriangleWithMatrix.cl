//function declaration

void rasterProject(int *out, double *v0, __global int *size);
bool isOutOfBounds(double *point);
int getTextureAt(int *texturePoint, __global int *textureSize, __global uint *texture);
void applyMatrix(__global double *matrix, double *in, bool _explicit, double *out);
//end function declaration

//t01 - 03:     the three points of the triangle in relative space
//tA01 - 03:    the three points of a triangle that map to the texture
//out:          a pixel grid reduced to 1 dimension
//zMap:         a grid of depth values reduced to 1 dimension

//NOTE: both global sizes should be the same
__kernel void drawTriangle(
    __global int *screenSize,
    __global uint *texture,
    __global int *textureSize,
    __global double *t01, __global double *t02, __global double *t03,
    __global double *tA01, __global double *tA02, __global double *tA03,
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

        double p[3] = {
            (u * t01[0]) + (v * t02[0]) + (w * t03[0]),
            (u * t01[1]) + (v * t02[1]) + (w * t03[1]),
            (u * t01[2]) + (v * t02[2]) + (w * t03[2])
        };
        double p_out[3] = {0, 0, 0};

        applyMatrix(iMatrix, p, false, p_out);//use p, write into p_out
        applyMatrix(eMatrix, p_out, true, p);//use p_out, write into p

        if (!isOutOfBounds(p))
        {
            int raster[2];
            rasterProject(raster, p, screenSize);

            int texturePoint[2] = {
                (int)(textureSize[0] * ((u * tA01[0]) + (v * tA02[0]) + (w * tA03[0]))),
                (int)(textureSize[1] * ((u * tA01[1]) + (v * tA02[1]) + (w * tA03[1])))
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