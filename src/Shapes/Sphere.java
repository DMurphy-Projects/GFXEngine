package Shapes;

import java.awt.Color;

import Shapes.Shape2D.Circle;

public class Sphere extends Circle {

	public Sphere(Color c) {
		super(c);
	}

	@Override
	protected void createShape() {
		for (int o = 0; o < 2; o++) {
			orientation = o;
			double x , y, z, width;
			x = y = z = 0;
			width = 1;
			double cX, cY, cZ, pX = x, pY = y, pZ = z, rad = width;
			for (double i = 0; i < Math.PI; i += (Math.PI / 32)) {
				if (orientation == 0) {
					cX = x + (rad * Math.sin(i));
					z = pZ + (rad * Math.cos(i));
					width = Math.abs(cX - x);
				} else if (orientation == 1) {
					cY = y + (rad * Math.sin(i));
					x = pX + (rad * Math.cos(i));
					width = Math.abs(cY - y);
				} else {
					cZ = z + (rad * Math.sin(i));
					y = pY + (rad * Math.cos(i));
					width = Math.abs(cZ - z);
				}

				if (width < 0.0001)
					continue;
				super.createShape();
			}
		}
	}
}
