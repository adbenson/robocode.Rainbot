package net.adbenson.utility;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class Vector {
		
	public final double x;
	public final double y;
	
	static final double FULL_CIRCLE = 2 * Math.PI;
	
	public Vector() {
		x = 0;
		y = 0;
	}
	
	public Vector invert() {
		return new Vector(-x, -y);
	}

	public double getAngle() {
		Vector normal = this.normalize();
		double angle = (FULL_CIRCLE - Math.atan2(normal.x, normal.y));				
		return angle;
	}

	public Vector normalize() {
		double length = this.magnitude();
		if (length != 0) {
			return new Vector(x / length, y / length);
		}
		else {
			return new Vector(0, 0);
		}
	}
	
	public double magnitude() {
		return toPoint().distance(0, 0);
	}

	public Vector(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public Vector(Rectangle2D rect) {
		this(rect.getWidth(), rect.getHeight());
	}

	public Vector scale(double delta) {
		return new Vector(x*delta, y*delta);
	}
	
	public Vector add(Vector position) {
		return add(position.x, position.y);
	}
	
	public Vector subtract(Vector vector) {
		return new Vector(this.x - vector.x, this.y - vector.y);
	}
	
	public int intX() {
		return (int) Math.round(x);
	}
	
	public int intY() {
		return (int) Math.round(y);
	}

	public Vector add(double x, double y) {
		return new Vector(this.x + x, this.y + y);
	}

	public double normalDistance(Vector that) {
		Vector thisNorm = this.normalize();
		Vector thatNorm = that.normalize();
		Vector difference = thisNorm.subtract(thatNorm);
		return difference.magnitude();
	}
	
	public static Vector getVectorFromAngle(double angle, double length) {
		
		double x = Math.sin(angle) * length;
		double y = Math.cos(angle) * length;

		return new Vector(x, y);
	}

	public Point2D toPoint() {
		return new Point2D.Double(x, y);
	}
	
	public void drawTo(Graphics2D g, double angle, double length) {
		Vector end = this.project(angle, length);
		g.drawLine(intX(), intY(), end.intX(), end.intY());
	}
	
	public Vector project(double angle, double length) {
		return this.add(getVectorFromAngle(angle, length));
	}

	public double distance(Vector position) {
		double xDiff = position.x - this.x;
		double yDiff = position.y - this.y;
		
		return Math.sqrt((xDiff * xDiff) + (yDiff * yDiff));
	}
	
	public String toString() {
		return "Vector: ("+x+", "+y+") ["+hashCode()+"]";
	}

}