//function declaration
long swapLong(long var0);
double longBitsToDouble(long bits);
//end function declaration

__kernel void testWrite(__global double *in, __global double *out)
{
    int pos = get_local_size(0) * get_group_id(0) + get_local_id(0);
//    out[pos] = longBitsToDouble(swapLong(in[pos]));
    out[pos] = in[pos];
}

long swapLong( long val )
{
    val = ((val << 8) & 0xFF00FF00FF00FF00L ) | ((val >> 8) & 0x00FF00FF00FF00FFL );
    val = ((val << 16) & 0xFFFF0000FFFF0000L ) | ((val >> 16) & 0x0000FFFF0000FFFFL );
    return (val << 32) | (val >> 32);
}

//from java: Double.longBitsToDouble(long) implementation
double longBitsToDouble(long bits)
{
    int s = ((bits >> 63) == 0) ? 1 : -1;
    int e = (int)((bits >> 52) & 0x7ffL);
    long m = (e == 0) ?
                 (bits & 0xfffffffffffffL) << 1 :
                 (bits & 0xfffffffffffffL) | 0x10000000000000L;
    return s * m * pown((double)2, (int)(e-1075));
}