package Shapes.Rubix;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import GxEngine3D.Polygon3D;
import GxEngine3D.Plane;
import GxEngine3D.RefPoint3D;
import GxEngine3D.CalculationHelper.RotationCalc;
import GxEngine3D.CalculationHelper.VectorCalc;
import GxEngine3D.ViewHandler;
import Shapes.Cube;

public class RubixCube extends Cube implements KeyListener {

	private RubixCubelet[][][] cube;

	private double tol = 0.1;

	private boolean[] keys = new boolean[2];

	public RubixCube(double x, double y, double z, double width, double length,
			double height, Color c, ViewHandler v) {
		super(x, y, z, width, length, height, c, v);
	}

	private ArrayList<RefPoint3D> createBlock(int x, int y, int z) {
		Color[] c = new Color[6];
		if (z == 0)
		{
			c[0] = Color.YELLOW;
		}
		if (z == 2)
		{
			c[1] = Color.WHITE;
		}
		if (y == 0)
		{
			c[4] = new Color(255, 88, 0);//better orange
		}
		if (y == 2)
		{
			c[5] = Color.RED;
		}
		if (x == 0)
		{
			c[3] = Color.GREEN;
		}
		if (x == 2)
		{
			c[2] = Color.BLUE;
		}
		
		for (int i=0;i<6;i++)
		{
			if (c[i]==null)
			{
				c[i] = this.c;
			}
		}
		if (points.size() == 0) {
			super.createShape(c);
		}
		ArrayList<RefPoint3D> block = (ArrayList<RefPoint3D>) points.clone();
		points.clear();

		return block;
	}

	@Override
	protected void createShape() {
		x -= width;
		y -= length;
		z -= height;

		double startX = x;
		double startY = y;

		cube = new RubixCubelet[3][3][3];

		for (int l = 0; l < 3; l++) {
			for (int h = 0; h < 3; h++) {
				for (int w = 0; w < 3; w++) {
					cube[w][h][l] = new RubixCubelet(createBlock(w, h, l));
					x += width;
				}
				x = startX;
				y += length;
			}
			y = startY;
			z += height;
		}

		setRotation(0, 3, 0, 3, 0, 3);
	}

	private void setRotation(int ls, int le, int hs, int he, int ws, int we) {
		points.clear();
		for (int l = ls; l < le; l++) {
			for (int h = hs; h < he; h++) {
				for (int w = ws; w < we; w++) {
					for (RefPoint3D p : cube[w][h][l].Cube()) {
						points.add(p);
					}
				}
			}
		}
	}

	private boolean animation = false;
	private int animationCycles = 32;
	private int curCycle = 0;
	private int side = 0;

	private void rotatePart() {
		if (animation) {
			partRotation(side);
			return;
		}
		if (keys[0]) {
			animation = true;
			partRotation(or);
		} else if (keys[1]) {
			animation = true;
			if (or == 0) {
				partRotation(1);
			} else if (or == 1) {
				partRotation(2);
			} else if (or == 2) {
				partRotation(1);
			}
		}
	}

	double t1, t2, t3, t4, t5, t6, t7, t8, t9;

	private void partRotation(int i) {
		if (curCycle == 0) {
			// printCube();
			t1 = rotXY;
			t2 = rotXZ;
			t3 = rotYZ;
			t4 = tXY;
			t5 = tXZ;
			t6 = tYZ;
			t7 = lookAt.X();
			t8 = lookAt.Y();
			t9 = Math.round(lookAt.Z());
			side = i;
		}
		if (curCycle >= animationCycles) {
			// printCube();
			animation = false;
			rotXY = t1;
			rotXZ = t2;
			rotYZ = t3;
			tXY = t4;
			tXZ = t5;
			tYZ = t6;
			curCycle = 0;
			if (i == 0) {
				// setRotation(0, 3, 0, 3, 0, 3);
				yaw((int) t9, Math.PI / 2);
			} else if (i == 1) {
				pitch((int) t8, Math.PI / 2);
			} else if (i == 2) {
				roll((int) t7, Math.PI / 2);
			}
		} else {
			if (i == 0) {
				setRotation((int) t9, (int) t9 + 1, 0, 3, 0, 3);
				yaw(Math.PI / 2 / animationCycles);
			} else if (i == 1) {
				setRotation(0, 3, (int) t8, (int) t8 + 1, 0, 3);
				pitch(Math.PI / 2 / animationCycles);
			} else if (i == 2) {
				setRotation(0, 3, 0, 3, (int) t7, (int) t7 + 1);
				roll(Math.PI / 2 / animationCycles);
			}
			super.update();
			setRotation(0, 3, 0, 3, 0, 3);// set rotation back to normal
			curCycle += 1;
		}
	}

	private void printCube() {
		for (int l = 0; l < 1; l++)
			for (int h = 0; h < 3; h++)
				for (int w = 0; w < 3; w++)
					System.out.println(cube[w][h][l].Centre());
		System.out.println();
	}

	private void pitch(int t, double angle) {
		RubixCubelet[][][] copy = new RubixCubelet[3][3][3];
		int centre = 1;
		double a = 0;
		double l = 0, h = 0;

		copy[1][t][1] = cube[1][t][1];
		// printCube();
		// System.out.println();
		while (a < Math.PI * 2) {
			h = Math.round(Math.cos(a)) + centre;
			l = Math.round(Math.sin(a)) + centre;
			// System.out.println(h + " " + l);
			a += Math.PI / 4;

			double[] r = RotationCalc.rotateFull(h, l, 0, centre, centre, 0, angle, 0, 0);
			r[0] = Math.round(r[0]);
			r[1] = Math.round(r[1]); //

			copy[(int) h][t][(int) l] = cube[(int) r[0]][t][(int) r[1]];
		}

		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				for (int z = 0; z < 3; z++) {
					if (y == t)
						continue;
					copy[x][y][z] = cube[x][y][z];
				}
			}
		}
		cube = copy;

		// System.out.println(h + " " + l + " " + r[0] + " " + r[1]);
	}

	private void roll(int t, double angle) {
		RubixCubelet[][][] copy = new RubixCubelet[3][3][3];
		int centre = 1;
		double a = 0;
		double l = 0, h = 0;

		copy[t][1][1] = cube[t][1][1];
		// printCube();
		// System.out.println();
		while (a < Math.PI * 2) {
			h = Math.round(Math.cos(a)) + centre;
			l = Math.round(Math.sin(a)) + centre;
			// System.out.println(h + " " + l);
			a += Math.PI / 4;

			double[] r = RotationCalc.rotateFull(h, l, 0, centre, centre, 0, angle, 0, 0);
			r[0] = Math.round(r[0]);
			r[1] = Math.round(r[1]); //

			copy[t][(int) h][(int) l] = cube[t][(int) r[0]][(int) r[1]];
		}

		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				for (int z = 0; z < 3; z++) {
					if (x == t)
						continue;
					copy[x][y][z] = cube[x][y][z];
				}
			}
		}
		cube = copy;

		// System.out.println(h + " " + l + " " + r[0] + " " + r[1]);
	}

	private void yaw(int t, double angle) {
		RubixCubelet[][][] copy = new RubixCubelet[3][3][3];
		int centre = 1;
		double a = 0;
		double l = 0, h = 0;

		copy[1][1][t] = cube[1][1][t];
		// printCube();
		// System.out.println();
		while (a < Math.PI * 2) {
			h = Math.round(Math.cos(a)) + centre;
			l = Math.round(Math.sin(a)) + centre;
			// System.out.println(h + " " + l);
			a += Math.PI / 4;

			double[] r = RotationCalc.rotateFull(h, l, 0, centre, centre, 0, angle, 0, 0);
			r[0] = Math.round(r[0]);
			r[1] = Math.round(r[1]); //

			copy[(int) h][(int) l][t] = cube[(int) r[0]][(int) r[1]][t];
		}

		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				for (int z = 0; z < 3; z++) {
					if (z == t)
						continue;
					copy[x][y][z] = cube[x][y][z];
				}
			}
		}
		cube = copy;
		// System.out.println(h + " " + l + " " + r[0] + " " + r[1]);
	}

	@Override
	public void update() {
		super.update();
		// printCube();rotate
		rotatePart();
	}

	RefPoint3D lookAt;
	int or = 0;

	@Override
	public void hover(Polygon3D o) {
		super.hover(o);
		double[] c = findCentre();
		Plane plane = new Plane(o);
		double[] pV = new double[] { plane.getNV().X(), plane.getNV().Y(), plane.getNV().Z() };
		double[] pI = VectorCalc.isect_vec_plane_perspective(pV, c, plane.getP(), pV).Point();//TODO
		pI = VectorCalc.sub_v3v3(pI, c);
		
		pI = RotationCalc.rotateFull(pI[0], pI[1], pI[2], 0, 0, 0, rotXY, rotXZ, rotYZ);
		
		if (pI[0] > tol) {
			or = 2;// top
		} else if (pI[0] < -tol) {
			or = 2;
		} else if (pI[1] > tol) {
			or = 1;// left
		} else if (pI[1] < -tol) {
			or = 1;
		} else if (pI[2] > tol) {
			or = 0;// front
		} else if (pI[2] < -tol) {
			or = 0;
		}
		// problem need to find centre of cubelet, as top of cubelet will be z+1
		// whilst side will be z
		// go through each cubelet and check if a point in o is a part of that
		// cube, if so get centre of that cube
		// System.out.println(c[0] + " " + c[1] + " " + c[2]);
		boolean b = false;
		for (int l = 0; l < 3; l++) {
			for (int h = 0; h < 3; h++) {
				for (int w = 0; w < 3; w++) {
					if (cube[l][h][w].is(o.getShape())) {
						lookAt = cube[l][h][w].Centre();
						b = true;
					}
					if (b)
						break;
				}
				if (b)
					break;
			}
			if (b)
				break;
		}
		lookAt.setX((lookAt.X() - c[0] + width) / width);
		lookAt.setX(Math.round(lookAt.X()));
		lookAt.setY((lookAt.Y() - c[1] + length) / length);
		lookAt.setY(Math.round(lookAt.Y()));
		lookAt.setZ((lookAt.Z() - c[2] + height) / height);
		lookAt.setZ(Math.round(lookAt.Z()));
		// System.out.println(lookAt);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_L)
			keys[0] = true;
		if (e.getKeyCode() == KeyEvent.VK_K)
			keys[1] = true;
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_L)
			keys[0] = false;
		if (e.getKeyCode() == KeyEvent.VK_K)
			keys[1] = false;

	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

}
