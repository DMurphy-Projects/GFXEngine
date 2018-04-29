package Programs;

import DebugTools.TextModule.GlobalCategoryBlacklist;
import DebugTools.TextModule.TextBlacklist;
import DebugTools.TextModule.TextToggle;
import GxEngine3D.CalculationHelper.Matrix;
import GxEngine3D.CalculationHelper.VectorCalc;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Controller.GXController;
import GxEngine3D.Controller.Scene;
import DebugTools.TextOutput;
import GxEngine3D.Lighting.Light;
import GxEngine3D.View.Screen;
import GxEngine3D.View.ViewController;
import GxEngine3D.View.ViewHandler;
import MenuController.LookMenuController;
import ObjectFactory.*;
import Shapes.*;
import Shapes.Shape2D.Line;
import Shapes.Shape2D.Sqaure;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GraphicsProgram {

	public static void main(String[] args) {
		TextOutput.setModule(
				new TextToggle(
						new TextBlacklist(
								new GlobalCategoryBlacklist(1, 2),
								Matrix.class.getName()),
						true));

		double[] lightLocation = {0, 0, 4};

		Camera camera1 = new Camera(5, 5, 20);
		Camera camera2 = new Camera(5, 5, 20);
		Camera camera3 = new Camera(5, 5, 20);

		Line ln = new Line(0, 0, 0, lightLocation[0], lightLocation[1], lightLocation[2]);
		Light ls = new Light(lightLocation[0], lightLocation[1], lightLocation[2], 10, ln);

		final Scene scene = new Scene(ls);
//		scene.addObject(ln);//shows where the light is, not where its actually shining

		//testing for matrix plane intersections
		Sqaure sq01 = new Sqaure(2.5, 2.5, 1.5, 2, Color.BLUE);
		sq01.yaw(Math.toRadians(45));
		scene.addObject(sq01);

		Cube cube = new Cube(1, 1, 1, 1, 1, 1, Color.RED);
		scene.addObject(cube);

		ViewController viewCon = new ViewController();
		Screen panel1 = new Screen();
		panel1.setPreferredSize(new Dimension(500, 500));

		Screen panel2 = new Screen();
		panel2.setPreferredSize(new Dimension(500, 500));

		Screen panel3 = new Screen();
		panel3.setPreferredSize(new Dimension(500, 500));
		//-----View handler setup
		ViewHandler vH;
		vH = viewCon.add(panel1, camera1, scene);
		panel1.setHandler(vH);

		vH = viewCon.add(panel2, camera1, scene);
		panel2.setHandler(vH);

		vH = viewCon.add(panel3, camera3, scene);
		panel3.setHandler(vH);
		//-----View handler end

		final ShapeFactory factory = new ShapeFactory();
		JMenu lookMenu = new JMenu("Look At");
		final LookMenuController lookCon = new LookMenuController();

		final GXController gCon = new GXController(viewCon);
		addListeners(panel1, gCon);
		addListeners(panel2, gCon);
		addListeners(panel3, gCon);

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

		JFrame frame = setupFrame(panel1, "Panel 1");
		frame.setJMenuBar(menuBar);

//		setupFrame(panel2, "Panel 2");
//		setupFrame(panel3, "Panel 3");

		lookCon.updateMenu(lookMenu, scene, actions);
		gCon.setup();
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
