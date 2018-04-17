package Programs;

import GxEngine3D.CalculationHelper.Matrix;
import GxEngine3D.CalculationHelper.VectorCalc;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Controller.GXController;
import GxEngine3D.Controller.Scene;
import GxEngine3D.Lighting.Light;
import GxEngine3D.Model.Plane;
import GxEngine3D.Model.Polygon3D;
import GxEngine3D.Model.RefPoint3D;
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
import java.util.ArrayList;
import java.util.Arrays;

public class GraphicsProgram {

	public static void main(String[] args) {
		final ViewHandler vH = new ViewHandler();

		double[] lightLocation = {0, 0, 0};

		final Camera camera = new Camera(5, 5, 20, vH);
		Line ln = new Line(0, 0, 0, lightLocation[0], lightLocation[1], lightLocation[2], vH);
		Light ls = new Light(lightLocation[0], lightLocation[1], lightLocation[2], 1, ln);

		final Scene scene = new Scene(camera, ls, 0);

		Screen panel = new Screen(scene);
		panel.setPreferredSize(new Dimension(500, 500));
		
		vH.setPanel(panel);

		//testing for matrix plane intersections
		Sqaure sq01 = new Sqaure(0, 0, 2, 5, Color.white, vH);
		scene.addObject(sq01);

		Sqaure sq02 = new Sqaure(0, 0, 2, 2, Color.white, vH);
		sq02.roll(Math.toRadians(90));
		sq02.pitch(Math.toRadians(20));
		sq02.yaw(Math.toRadians(45));
		scene.addObject(sq02);

		Sqaure sq03 = new Sqaure(0, 0, 2, 5, Color.white, vH);
		sq03.roll(Math.toRadians(90));
		sq03.pitch(Math.toRadians(90));
		sq03.yaw(Math.toRadians(45));
//		scene.addObject(sq03);

		scene.update();

		Plane plane0 = new Plane(sq01.getShape().get(0));
		Plane plane1 = new Plane(sq02.getShape().get(0));
		Plane plane2 = new Plane(sq03.getShape().get(0));

		double[] p1 = new double[]{1, 0, -10};
		double[] p2 = new double[]{1, 0, 10};

		//so a plane counts as 1 but a line counts as 2
		Matrix m = new Matrix(3, 4);
		m.addEqautionOfPlane(plane0);
		m.addEqautionOfPlane(plane1);
//		m.addEqautionOfLine(p1, p2);

//		m.seteDebug(true);
		m.gaussJordandElimination();
		m.determineSolution();

		//squares are treated as planes so isn't collision detection for squares but for planes
		if (m.getSolutionType() == Matrix.SolutionType.POINT) {
			double[] point = m.getPointSolution();
			Cube cube = new Cube(point[0], point[1], point[2], 0.1, 0.1, 0.1, Color.RED, vH);
			scene.addObject(cube);
		}
		else if (m.getSolutionType() == Matrix.SolutionType.LINE)
		{
			double[] prev = null;
			Color[] c = new Color[]{
					Color.RED,
					Color.BLUE,
					Color.GREEN,
					Color.YELLOW
			};
			int cIndex = 0;
			//so edge intersects also get the intersections from the ray that the edge makes, therefor some intersections appear outside the shape but is still correct
			//for this method, to remove this we must check if the intersection points are inside/on the polygon
			//similar to plane-sided but with edges
			for (RefPoint3D[] edge:sq01.getEdges())
			{
				//two lines so has to be 4 length
				Matrix edgeIntersect = new Matrix(m.getRows()+2, 4);
				edgeIntersect.addMatrixOfEqautions(m);
				edgeIntersect.addEqautionOfLine(edge[0].toArray(), edge[1].toArray());
				edgeIntersect.gaussJordandElimination();
				edgeIntersect.determineSolution();
				System.out.println(edgeIntersect.getSolutionType());
				System.out.println(edgeIntersect);
				if (edgeIntersect.getSolutionType() == Matrix.SolutionType.POINT)
				{
					double[] p = edgeIntersect.getPointSolution();
					if (prev == null)
					{
						prev = p;
					}
					else
					{
						Line intersectLine = new Line(prev[0], prev[1], prev[2], p[0], p[1], p[2], vH);
						scene.addObject(intersectLine);
					}
					Cube iCube = new Cube(p[0], p[1], p[2], 0.1, 0.1, 0.1, c[cIndex], vH);
					scene.addObject(iCube);
					cIndex++;
				}
			}
		}
		//end matrix test

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
