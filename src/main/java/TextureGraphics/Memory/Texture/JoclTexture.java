package TextureGraphics.Memory.Texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;

public class JoclTexture implements ITexture {

    private static int _id = 0;
    int id;

    protected BufferedImage image;

    ITextureData dataHandler;

    public JoclTexture(String path, ITextureData dataHandler)
    {
        try {
            BufferedImage image = ImageIO.read(new File(getClass().getClassLoader().getResource(path).getFile()));
            init(image, dataHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JoclTexture(BufferedImage image, ITextureData dataHandler)
    {
        init(image, dataHandler);
    }

    private void init(BufferedImage image, ITextureData dataHandler)
    {
        this.dataHandler = dataHandler;

        this.image = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        this.image.getGraphics().drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);

        int[] dataArray = ((DataBufferInt) this.image.getRaster().getDataBuffer()).getData();

        dataHandler.create(image.getWidth(), image.getHeight(), dataArray, this);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public ITextureData getDataHandler() {
        return dataHandler;
    }
}
