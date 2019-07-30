
__kernel void testNoCull(int maxSize, int size, __global double *out1, __global double *out2)
{
    int globalSize = get_global_size(0);
    int div = maxSize / globalSize;
    int pos = div * (get_local_size(0) * get_group_id(0) + get_local_id(0));

    for (int i=0;i<div;i++)
    {
        double index = pos + i + 1;
        double approx = round(native_sqrt(index*2));

        double x = (approx*(approx+1)/2) - index;
        double y = (approx - 1) - x;
        x = x / size;
        y = y / size;

        out1[pos+i] = x;
        out2[pos+i] = y;
    }
}

__kernel void testCull(__global double *out1, __global double *out2)
{
    double iu = get_local_size(0) * get_group_id(0) + get_local_id(0);
    double iv = get_local_size(1) * get_group_id(1) + get_local_id(1);
    double size = get_global_size(0);

    if (iv + iu < size)
    {
        double u = iu / size;
        double v = iv / size;

        out1[(int)iu] = u;
        out2[(int)iv] = v;
    }
}