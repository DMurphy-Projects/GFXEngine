__kernel void render(
    __global int *screenSize,
    __global int *outMap,
    __global int *outPixel
)
{
    int x = get_local_size(0) * get_group_id(0) + get_local_id(0);
    int y = get_local_size(1) * get_group_id(1) + get_local_id(1);
    int pos = (y * screenSize[0]) + x;

    int localIndex = outMap[(pos * 2) + 0];
    if (localIndex < 0)
    {
       //there is nothing to render, so fill with solid black
       outPixel[pos] = 0;
    }
}
