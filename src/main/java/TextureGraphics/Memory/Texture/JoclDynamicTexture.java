package TextureGraphics.Memory.Texture;

import Games.IGameScreen;
import TextureGraphics.Memory.JoclMemoryMethods;
import org.jocl.*;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import static org.jocl.CL.*;

public class JoclDynamicTexture implements ITexture, IGameScreen {

    cl_context context;
    cl_command_queue commandQueue;

    int tWidth, tHeight;

    protected cl_mem textureMem, textureSizeMem;

    BufferedImage texture;

    public JoclDynamicTexture(JoclTexture texture)
    {
        context = texture.context;
        commandQueue = texture.commandQueue;

        tWidth = texture.tWidth;
        tHeight = texture.tHeight;

        this.texture = new BufferedImage(tWidth, tHeight, BufferedImage.TYPE_INT_RGB);
        this.texture.getGraphics().drawImage(texture.image, 0, 0, tWidth, tHeight, null);

        update();
    }

    protected void asyncSetupTexture(BufferedImage texture) {
        tWidth = texture.getWidth();
        tHeight = texture.getHeight();

        cl_event[] events = new cl_event[]{new cl_event(), new cl_event()};
        cl_mem size = JoclMemoryMethods.asyncWrite(context, commandQueue, new int[]{tWidth, tHeight}, events[0], CL_MEM_READ_ONLY);

        int[] textureArr = texture.getRGB(0, 0, texture.getWidth(), texture.getHeight(), null, 0, texture.getWidth());
        cl_mem tex = JoclMemoryMethods.asyncWrite(context, commandQueue, textureArr, events[1], CL_MEM_READ_ONLY);

        clWaitForEvents(events.length, events);
        this.textureMem = tex;
        this.textureSizeMem = size;
    }

    public void update()
    {
        asyncSetupTexture(this.texture);
    }

    @Override
    public int[] getScreenContents() {
        DataBufferInt dataBuffer = (DataBufferInt) texture.getRaster().getDataBuffer();
        return dataBuffer.getData();
    }

    @Override
    public int[] getScreenSize() {
        return new int[]{ tWidth, tHeight };
    }

    @Override
    public Pointer getTexture() {
        return Pointer.to(textureMem);
    }

    @Override
    public Pointer getSize() {
        return Pointer.to(textureSizeMem);
    }
}
