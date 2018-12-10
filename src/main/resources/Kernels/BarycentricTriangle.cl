//function declaration

void rasterProject(int *out, double *v0, __global int *size);
bool isOutOfBounds(int *raster, __global int *size);
int getTextureAt(int *texturePoint, __global int *textureSize, __global uint *texture);
//end function declaration

//t01 - 03:     the three points of the triangle in relative space
//tA01 - 03:    the three points of a triangle that map to the texture
__kernel void drawTriangle(
    __global uint *output,
    __global int *screenSize,
    __global uint *texture,
    __global int *textureSize,
    __global double *t01, __global double *t02, __global double *t03,
    __global double *tA01, __global double *tA02, __global double *tA03,
     __global double *debug
    )
{

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