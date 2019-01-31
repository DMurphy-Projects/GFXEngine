
__kernel void testNoCull(int size, __global double *out1, __global double *out2)
{
    int globalSize = get_global_size(0);
    int div = size / globalSize;
    int pos = div * (get_local_size(0) * get_group_id(0) + get_local_id(0));


    for (int i=0;i<div;i++)
    {
        double index = pos + i + 1;
        double approx = round(native_sqrt(index*2));

        double x = (approx*(approx+1)/2) - index;
        out1[pos+i] = x;
        out2[pos+i] = (approx - 1) - x;
    }
}