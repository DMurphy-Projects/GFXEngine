package Programs;

import DebugTools.TextModule.GlobalCategoryBlacklist;
import DebugTools.TextModule.TextBlacklist;
import DebugTools.TextModule.TextToggle;
import DebugTools.TextOutput;
import GxEngine3D.CalculationHelper.Matrix;
import GxEngine3D.CalculationHelper.VectorCalc;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Controller.GXController;
import GxEngine3D.Controller.Scene;
import GxEngine3D.Lighting.Light;
import GxEngine3D.Ordering.SidedOrdering;
import GxEngine3D.View.*;
import MenuController.LookMenuController;
import ObjectFactory.*;
import Shapes.BaseShape;
import Shapes.FakeSphere;
import Shapes.Shape2D.Line;
import Shapes.Shape2D.Sqaure;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LightningTest {

    public static void main(String[] args) {
        TextOutput.setModule(
                new TextToggle(
                        new TextBlacklist(
                                new GlobalCategoryBlacklist(1, 2),
                                Matrix.class.getName()),
                        true));

        double[] lightLocation = {0, 0, 5};

        Camera camera1 = new Camera(5, 5, 20);

        Line ln = new Line(0, 0, 0, lightLocation[0], lightLocation[1], lightLocation[2]);
        Light ls = new Light(lightLocation[0], lightLocation[1], lightLocation[2], 10, ln);

        final Scene scene = new Scene(ls, new SidedOrdering());
        scene.setSplitting(true);
//		scene.addObject(ln);//shows where the light is, not where its actually shining
        scene.addObject(new FakeSphere(lightLocation[0], lightLocation[1], lightLocation[2], 1, Color.YELLOW));

        //testing for matrix plane intersections
        Sqaure sq01 = new Sqaure(1, 0, 0, 2, Color.BLUE);
        sq01.yaw(Math.toRadians(45));
        scene.addObject(sq01);

        sq01 = new Sqaure(0, 5, 5, 2, Color.RED);
        sq01.yaw(Math.toRadians(45));
        sq01.roll(Math.toRadians(90));
        scene.addObject(sq01);

        sq01 = new Sqaure(0, -5, 5, 2, Color.GREEN);
        sq01.yaw(Math.toRadians(45));
        sq01.roll(Math.toRadians(90));
        scene.addObject(sq01);

        sq01 = new Sqaure(5, 0, 5, 2, Color.CYAN);
        sq01.yaw(Math.toRadians(45));
        sq01.pitch(Math.toRadians(90));
        scene.addObject(sq01);

        sq01 = new Sqaure(-5, 0, 5, 2, Color.MAGENTA);
        sq01.yaw(Math.toRadians(45));
        sq01.pitch(Math.toRadians(90));
        scene.addObject(sq01);

        sq01 = new Sqaure(0, 0, 10, 2, Color.WHITE);
        sq01.yaw(Math.toRadians(45));
        scene.addObject(sq01);

        ViewController viewCon = new ViewController();
        //pip panels don't actually get drawn its just used as a container for dimensions
        Screen pipPanel = new Screen();
        pipPanel.setPreferredSize(new Dimension(500, 500));

        final GXController gCon = new GXController(viewCon);
        addListeners(pipPanel, gCon);

        //-----View handler setup
        ViewHandler vH;
        vH = viewCon.add(pipPanel, camera1, scene);
        gCon.add(vH);

        pipPanel.setHandler(vH);
        //-----Picture in picture setup end
        //-----View handler end

        final ShapeFactory factory = new ShapeFactory();
        JMenu lookMenu = new JMenu("Look At");
        final LookMenuController lookCon = new LookMenuController();

        ActionListener actions = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String act = e.getActionCommand();
                Camera camera = viewCon.getActive().getCamera();
                if (act.startsWith("spawn")) {
                    double[] l = VectorCalc.add_v3v3(camera.position(), VectorCalc
                            .mul_v3_fl(camera.direction(), 0.01d * viewCon.getActive().getZoom()));
                    scene.addObject(factory.createObject(
                            Integer.parseInt(act.split(":")[1]), l[0], l[1], l[2]));
                    lookCon.updateMenu(lookMenu, scene, this);
                } else if (act.startsWith("look")) {
                    camera.lookAt((BaseShape) scene.getShapes().get(
                            Integer.parseInt(act.split(":")[1])));
                    gCon.centreMouse();
                } else
                    return;
            }
        };

        //-----shapes
        factory.add(new CubeProduct());
        factory.add(new CircleProduct());
        factory.add(new PrismProduct());
        factory.add(new PyramidProduct());
        factory.add(new FakeSphereProduct());
        //-----end shapes

        // main window with menu bar
        JMenuBar menuBar = new JMenuBar();
        // start fill menu
        JMenu menu = new JMenu("Objects");
        menuBar.add(menu);

        JMenu sub = new JMenu("Spawn");
        int count = 0;
        for (IProduct ip : factory.shapeList()) {
            JMenuItem menuItem = new JMenuItem(ip.Name());
            menuItem.setActionCommand("spawn:" + count);
            count++;
            menuItem.addActionListener(actions);
            sub.add(menuItem);
        }
        menu.add(sub);

        menu = new JMenu("View");
        menuBar.add(menu);

        menu.add(lookMenu);

        JFrame frame = setupFrame(pipPanel, "Panel 1");
        frame.setJMenuBar(menuBar);

//		setupFrame(panel2, "Panel 2");
//		setupFrame(panel3, "Panel 3");

        for (ViewHandler _vH:viewCon.getHandlers())
        {
            _vH.getCamera().lookAt((BaseShape) vH.getScene().getShapes().get(0));
            _vH.getCamera().setup();
        }

        lookCon.updateMenu(lookMenu, scene, actions);
        while (true) {
            gCon.update();
        }
    }

    public static void addListeners(JPanel p, GXController gCon)
    {
        p.addKeyListener(gCon);
        p.addMouseMotionListener(gCon);
        p.addMouseWheelListener(gCon);
    }

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

