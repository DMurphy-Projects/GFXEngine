
void applyMatrix(__global double *matrix, double *in, double *out);
int getTextureAt(int *texturePoint, int width, int height, __global int *texture, int offset);

//textureDataArray: contains a list of variable length textures
//textureInfoArray: [memoryOffset, width, height]
//textureMetaInfo: contains an index for each polygon pointing to a texture that polygon uses is any
//relativeArray: contains points which make up a list of polygons in relative-space
//textureRelativeArray: contains points which make up a list of polygons in texture-space
//polygonArray: contains indices which correspond to points in the relativeArray, this defines the polygons
__kernel void render(
    __global int *screenSize,
    __global int *outMap,
    __global int *metaInfo,
    __global int *outPixel,
    __global int *indexArray,
    __global int *textureDataArray,
    __global int *textureInfoArray,
    __global int *textureMetaInfo,
    __global double *zMap,
    __global double *relativeArray,
    __global int *polygonArray,
    __global double *textureRelativeArray,
    __global int *polygonStartArray,
    __global double *inverse
)
{
    int x = get_local_size(0) * get_group_id(0) + get_local_id(0);
    int y = get_local_size(1) * get_group_id(1) + get_local_id(1);
    int pos = (y * screenSize[0]) + x;

    int localIndex = outMap[(pos * 2) + 0];
    if (localIndex >= 0)
    {
        int polygonIndex = indexArray[localIndex];

        if (metaInfo[polygonIndex] == 1)
        {
            int textureInfoIndex = textureMetaInfo[polygonIndex];
            //get info for texture
            int width = textureInfoArray[(textureInfoIndex * 3) + 0];
            int height = textureInfoArray[(textureInfoIndex * 3) + 1];
            int memoryOffset = textureInfoArray[(textureInfoIndex * 3) + 2];

            double _x = x;
            double _y = y;
            //convert (x, y) from screen-space to clip-space
            double clip_x = (_x / (screenSize[0] * 0.5)) - 1;
            double clip_y = (2 * (1 - (_y / screenSize[1]))) - 1;
            double z = zMap[pos];

            double p_in[3] = {clip_x, clip_y, z};
            //P
            double p_out[3] = {0, 0, 0};

            applyMatrix(inverse, p_in, p_out);

            //get the relevant relative-space triangle
            int polygonOffset = polygonStartArray[polygonIndex];
            int tIndex = outMap[(pos * 2) + 1];

            //A
            double p1x = relativeArray[polygonArray[polygonOffset + 0]];
            double p1y = relativeArray[polygonArray[polygonOffset + 1]];
            double p1z = relativeArray[polygonArray[polygonOffset + 2]];
            //B
            double p2x = relativeArray[polygonArray[polygonOffset + (tIndex*3) + 0]];
            double p2y = relativeArray[polygonArray[polygonOffset + (tIndex*3) + 1]];
            double p2z = relativeArray[polygonArray[polygonOffset + (tIndex*3) + 2]];
            //C
            double p3x = relativeArray[polygonArray[polygonOffset + ((tIndex+1)*3) + 0]];
            double p3y = relativeArray[polygonArray[polygonOffset + ((tIndex+1)*3) + 1]];
            double p3z = relativeArray[polygonArray[polygonOffset + ((tIndex+1)*3) + 2]];
            //TODO where did this come from, 3d barycentric co-ords?
            //calculate barycentric co-ords
            //denom = (dot00*dot11) - (dot01*dot01)
            //v = (dot00*dot12 - dot01*dot02) / denom
            //w = (dot11*dot02) - (dot01*dot12) / denom

            //calculate vectors
            double v0[3] = {p3x - p1x, p3y - p1y, p3z - p1z};//C-A
            double v1[3] = {p2x - p1x, p2y - p1y, p2z - p1z};//B-A
            double v2[3] = {p_out[0] - p1x, p_out[1] - p1y, p_out[2] - p1z};//P-A

            //prepare dot products for use
            double dot00 = (v0[0]*v0[0]) + (v0[1]*v0[1]) + (v0[2]*v0[2]);
            double dot01 = (v0[0]*v1[0]) + (v0[1]*v1[1]) + (v0[2]*v1[2]);
            double dot11 = (v1[0]*v1[0]) + (v1[1]*v1[1]) + (v1[2]*v1[2]);
            double dot02 = (v0[0]*v2[0]) + (v0[1]*v2[1]) + (v0[2]*v2[2]);
            double dot12 = (v1[0]*v2[0]) + (v1[1]*v2[1]) + (v1[2]*v2[2]);

            double denom = (dot00 * dot11) - (dot01 * dot01);
            double v = ((dot00 * dot12) - (dot01 * dot02)) / denom;
            double w = ((dot11 * dot02) - (dot01 * dot12)) / denom;
            double u = 1 - v - w;

            //use the barycentric co-ord to calculate the texture position
            int texturePoint[2] = {
                (int)(width *   (
                    (u * textureRelativeArray[polygonOffset + 0]) +
                    (v * textureRelativeArray[polygonOffset + (tIndex*3) + 0]) +
                    (w * textureRelativeArray[polygonOffset + ((tIndex+1)*3) + 0])
                    )),
                (int)(height *  (
                    (u * textureRelativeArray[polygonOffset + 1]) +
                    (v * textureRelativeArray[polygonOffset + (tIndex*3) + 1]) +
                    (w * textureRelativeArray[polygonOffset + ((tIndex+1)*3) + 1])
                    ))
            };
            int color = getTextureAt(texturePoint, width, height, textureDataArray, memoryOffset);
            outPixel[pos] = color;
        }
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

int getTextureAt(int *texturePoint, int width, int height, __global int *texture, int offset)
{
    if (texturePoint[0] < 0 || texturePoint[1] < 0 || texturePoint[0] >= width || texturePoint[1] >= height)
    {
        return (255 << 16) | (255 << 8 ) | 255;//white
    }
    int index = (texturePoint[1] * width) + texturePoint[0];
    return texture[index + offset];
}