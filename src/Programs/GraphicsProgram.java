package Programs;

import DebugTools.TextModule.GlobalCategoryBlacklist;
import DebugTools.TextModule.SpecificCategoryBlacklist;
import DebugTools.TextModule.TextBlacklist;
import DebugTools.TextModule.TextToggle;
import GxEngine3D.CalculationHelper.Matrix;
import GxEngine3D.CalculationHelper.VectorCalc;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Controller.GXController;
import GxEngine3D.Controller.Scene;
import DebugTools.TextOutput;
import GxEngine3D.Lighting.Light;
import GxEngine3D.View.PolygonIterator;
import GxEngine3D.View.Screen;
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
//								new SpecificCategoryBlacklist(PolygonIterator.class.getName(), 1),
								new GlobalCategoryBlacklist(1, 2),
								Matrix.class.getName()),
						true));
		final ViewHandler vH = new ViewHandler();

		double[] lightLocation = {0, 0, 4};

		final Camera camera = new Camera(5, 5, 20, vH);
		Line ln = new Line(0, 0, 0, lightLocation[0], lightLocation[1], lightLocation[2], vH);
		Light ls = new Light(lightLocation[0], lightLocation[1], lightLocation[2], 10, ln);

		final Scene scene = new Scene(camera, ls, 0);
//		scene.addObject(ln);//shows where the light is, not where its actually shining

		Screen panel = new Screen(scene);
		panel.setPreferredSize(new Dimension(500, 500));
		
		vH.setPanel(panel);

		//testing for matrix plane intersections
		Sqaure sq01 = new Sqaure(2.5, 2.5, 1.5, 2, Color.BLUE, vH);
		sq01.yaw(Math.toRadians(45));
		scene.addObject(sq01);

		Cube cube = new Cube(1, 1, 1, 1, 1, 1, Color.RED, vH);
		scene.addObject(cube);

		Sqaure sq02 = new Sqaure(1, 3, 1, 1, Color.GREEN, vH);
//		sq02.pitch(Math.toRadians(90));
//		scene.addObject(sq02);

//		Sqaure sq03 = new Sqaure(0, 0, 2, 5, Color.white, vH);
//		sq03.roll(Math.toRadians(90));
//		sq03.pitch(Math.toRadians(90));
//		sq03.yaw(Math.toRadians(45));
//		scene.addObject(sq03);
//		scene.update();


		final ShapeFactory factory = new ShapeFactory();
		final JMenu lookMenu = new JMenu("Look At");
		final LookMenuController lookCon = new LookMenuController();

		final GXController gCon = new GXController(scene, camera, panel, vH);

		ActionListener actions = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String act = e.getActionCommand();
				if (act.startsWith("spawn")) {
					double[] l = VectorCalc.add_v3v3(camera.position(), VectorCalc
							.mul_v3_fl(camera.direction(), vH.Zoom() / 100));
					scene.addObject(factory.createObject(
							Integer.parseInt(act.split(":")[1]), l[0], l[1], l[2],
							vH));
					lookCon.updateMenu(lookMenu, scene, this);
				} else if (act.startsWith("look")) {
					camera.lookAt((BaseShape) scene.getShapes().get(
							Integer.parseInt(act.split(":")[1])));
					gCon.CenterMouse();
				} else
					return;
			}
		};

		// shapes
		factory.add(new CubeProduct());
		factory.add(new CircleProduct());
		factory.add(new PrismProduct());
		factory.add(new PyramidProduct());
		factory.add(new FakeSphereProduct());
		// end shapes

		panel.addKeyListener(gCon);

		panel.addMouseListener(gCon);

		panel.addMouseMotionListener(gCon);
		panel.addMouseWheelListener(gCon);

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
		
		JFrame frame = new JFrame();
		frame.setJMenuBar(menuBar);
		frame.add(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		lookCon.updateMenu(lookMenu, scene, actions);
		gCon.setup();
		while (true) {
			gCon.update();
		}
	}

}
