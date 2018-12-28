package TextureGraphics.Memory;

import org.jocl.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.jocl.CL.CL_MEM_READ_ONLY;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clEnqueueWriteBuffer;

public class JoclTexture {

    private cl_mem textureMem, textureSizeMem;

    int tWidth, tHeight;

    public JoclTexture(String path, cl_context context, cl_command_queue commandQueue)
    {
        try {
            BufferedImage texture = ImageIO.read(new File(getClass().getClassLoader().getResource(path).getFile()));
            tWidth = texture.getWidth();
            tHeight = texture.getHeight();

            textureSizeMem = clCreateBuffer(context, CL_MEM_READ_ONLY,
                    2 * Sizeof.cl_int, null, null);
            clEnqueueWriteBuffer(commandQueue, textureSizeMem, true, 0,
                    2 * Sizeof.cl_int, Pointer.to(new int[]{tWidth, tHeight}), 0, null, null);

            int[] textureArr = texture.getRGB(0, 0, texture.getWidth(), texture.getHeight(), null, 0, texture.getWidth());
            textureMem = clCreateBuffer(context, CL_MEM_READ_ONLY,
                    tWidth * tHeight * Sizeof.cl_uint, null, null);
            clEnqueueWriteBuffer(commandQueue, textureMem, true, 0,
                    tWidth * tHeight * Sizeof.cl_uint, Pointer.to(textureArr), 0, null, null);


        } catch (IOException e) {
        }
    }

    public Pointer getTexture()
    {
        return Pointer.to(textureMem);
    }

    public Pointer getSize()
    {
        return Pointer.to(textureSizeMem);
    }
}
