
//outMap: contains information to calculate which triangle we're rendering, [polygonIndex, localTriangleIndex], ...
//metaInfo
__kernel void render(
    __global int *screenSize,
    __global int *outMap,
    __global int *metaInfo,
    __global int *colorArray,
    __global int *outPixel,
    __global int *indexArray
)
{
    int x = get_local_size(0) * get_group_id(0) + get_local_id(0);
    int y = get_local_size(1) * get_group_id(1) + get_local_id(1);
    int pos = (y * screenSize[0]) + x;

    int localIndex = outMap[(pos * 2) + 0];
    if (localIndex >= 0)
    {
        int polygonIndex = indexArray[localIndex];

        if (metaInfo[polygonIndex] == 0)
        {
            outPixel[pos] = colorArray[polygonIndex];
        }
    }
}
