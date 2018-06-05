package GxEngine3D.View.ViewHelper;

import java.awt.*;
import java.awt.image.BufferedImage;

public class InvisibleMouse {
    public static Cursor createCursor()
    {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        BufferedImage cursorImage = new BufferedImage(1, 1,
                BufferedImage.TRANSLUCENT);
        Cursor invisibleCursor = toolkit.createCustomCursor(cursorImage,
                new Point(0, 0), "InvisibleCursor");
        return invisibleCursor;
    }
}
