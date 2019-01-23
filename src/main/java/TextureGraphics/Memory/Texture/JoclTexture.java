package TextureGraphics.Memory.Texture;

import TextureGraphics.Memory.Texture.ITexture;
import org.jocl.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.jocl.CL.CL_MEM_READ_ONLY;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clEnqueueWriteBuffer;

public class JoclTexture implements ITexture {

    cl_context context;
    cl_command_queue commandQueue;

    protected cl_mem textureMem, textureSizeMem;

    BufferedImage image;

    int tWidth, tHeight;

    public JoclTexture(String path, cl_context context, cl_command_queue commandQueue)
    {
        this.context = context;
        this.commandQueue = commandQueue;

        try {
            image = ImageIO.read(new File(getClass().getClassLoader().getResource(path).getFile()));

            tWidth = image.getWidth();
            tHeight = image.getHeight();

            textureSizeMem = clCreateBuffer(context, CL_MEM_READ_ONLY,
                    2 * Sizeof.cl_int, null, null);
            clEnqueueWriteBuffer(commandQueue, textureSizeMem, true, 0,
                    2 * Sizeof.cl_int, Pointer.to(new int[]{tWidth, tHeight}), 0, null, null);

            int[] textureArr = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
            textureMem = clCreateBuffer(context, CL_MEM_READ_ONLY,
                    tWidth * tHeight * Sizeof.cl_uint, null, null);
            clEnqueueWriteBuffer(commandQueue, textureMem, true, 0,
                    tWidth * tHeight * Sizeof.cl_uint, Pointer.to(textureArr), 0, null, null);
        } catch (IOException e) {
            e.printStackTrace();
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
