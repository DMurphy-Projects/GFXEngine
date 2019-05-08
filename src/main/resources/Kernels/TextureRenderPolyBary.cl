//this is an implementation for drawing a triangle with a texture

//function declaration

bool isOutOfBounds(double x, double y, __global int *screenSize);
void applyMatrix(__global double *matrix, double *in, double *out);
double dot_v3(double v1X, double v1Y, double v1Z, double *v2);
int getTextureAt(int *texturePoint, __global int *textureSize, __global uint *texture);

//function end

//note:
//noPolygons:   the number of vertices in the polygon
//p?X, p?Y, p?Z: contains the (x, y, z) values of the polygon to render, ? is which space the poly resides in (R: relative, C: clip, S: screen)
//out:          a pixel grid reduced to 1 dimension
//zMap:         a grid of depth values reduced to 1 dimension
//planeEq:      [a, b, c, d] of the form, ax + by + cz + d = 0
//offset:       this kernel iterates over a rectangle region of the screen, this value moves that region
//inverse:      the inverse matrix which goes from clip-space -> relative-space for this polygon
//vectors:      contains the vectors required to calculate the barycentric co-ords in relative-space
//preCalc:      contains information, other than vectors, to calculate barycentric co-ords in relative-space
__kernel void drawTriangle(
    __global int *screenSize,
    __global uint *texture, __global int *textureSize,
    int noPolygons,
    __global double *pRX, __global double *pRY, __global double *pRZ,
    __global double *pCX, __global double *pCY, __global double *pCZ,
    __global double *pSX, __global double *pSY, __global double *pSZ,
    __global double *tAX, __global double *tAY,
    __global uint *out, __global double *zMap,
    __global double *planeEq, __global int *offset,
    __global double *inverse,
    __global double *vectors, __global double *preCalc
    )
{
    double x = (get_local_size(0) * get_group_id(0) + get_local_id(0)) + offset[0];
    double y = (get_local_size(1) * get_group_id(1) + get_local_id(1)) + offset[1];

    //http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
    bool inside = false;
    for ( int i = 0, j = noPolygons - 1 ; i < noPolygons ; j = i++ )
    {
        if ( ( pSY[i] > y ) != ( pSY[j] > y ) &&
            x < ( pSX[j] - pSX[i] ) * ( y - pSY[i] ) / ( pSY[j] - pSY[i] ) + pSX[i] )
        {
            inside = !inside;
        }
    }
    if (inside)
    {
        int pos = (y * screenSize[0]) + x;

        //we need to find z value, by using plane eq. ax + by +cz + d = 0, we can find (a, b, c, d) using triangle points
        //then substitute (x, y) and re-arrange for z, thus
        //z = (-ax - by - d) / c
        double z = (-(planeEq[0]*x) - (planeEq[1]*y) - planeEq[3]) / planeEq[2];
        if (z < 1 && z > 0 && zMap[pos] > z)
        {
            zMap[pos] = z;
            //calculate clip-space for (x, y), z is already in clip
            //screen_x = (clip_x + 1) * 0.5 * screenWidth
            //(screen_x / (screenWidth * 0.5)) - 1 = clip_x
            double clip_x = (x / (screenSize[0] * 0.5)) - 1;

            //screen_y =  (1 - (clip_y + 1)* 0.5) * screenHeight
            //(2 * (1 - (screen_y / screenHeight))) - 1 = clip_y
            double clip_y = (2 * (1 - (y / screenSize[1]))) - 1;


            //apply inverse to (x, y) to get relative-space points
            double p_in[3] = {clip_x, clip_y, z};
            double p_out[3] = {0, 0, 0};

            applyMatrix(inverse, p_in, p_out);//use p_in, write into p_out

            //find barycentric co-ords for relative space points
            int color = 0;
            int size = 4;
            for (int i=0;i < noPolygons-2;i++)
            {
                double v2[] = {p_out[0]-pRX[0], p_out[1]-pRY[0], p_out[2]-pRZ[0]};//P-A
                double dot02 = dot_v3(vectors[(i+1)*3], vectors[(i+1)*3+1], vectors[(i+1)*3+2], v2);
                double dot12 = dot_v3(vectors[i*3], vectors[i*3+1], vectors[i*3+2], v2);

                double v = ((preCalc[i*size] * dot12) - (preCalc[i*size+1] * dot02)) / preCalc[i*size+3];//(dot00*dot12 - dot01*dot02) / denom
                double w = ((preCalc[i*size+2] * dot02) - (preCalc[i*size+1] * dot12)) / preCalc[i*size+3];//(dot11*dot02) - (dot01*dot12) / denom

                //test if inside triangle
                if ((w >= 0) && (v >= 0) && (w + v < 1))
                {
                    double u = 1 - w - v;//u

                    int texturePoint[2] = {
                        (int)(textureSize[0] * ((u * tAX[0]) + (v * tAX[i+1]) + (w * tAX[i+2]))),
                        (int)(textureSize[1] * ((u * tAY[0]) + (v * tAY[i+1]) + (w * tAY[i+2])))
                    };
                    //map to texture space to get color
                    color = getTextureAt(texturePoint, textureSize, texture);
                    out[pos] = color;
                    return;
                }
            }

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

double dot_v3(double v1X, double v1Y, double v1Z, double *v2)
{
   return (v1X* v2[0]) + (v1Y*v2[1]) + (v1Z*v2[2]);
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