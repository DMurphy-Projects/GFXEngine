package GxEngine3D.Helper;

import javax.swing.*;

public class FrameHelper {
    public static JFrame setupFrame(JPanel panel, String title)
    {
        JFrame frame = new JFrame();
        frame.add(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle(title);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        return frame;
    }
}
