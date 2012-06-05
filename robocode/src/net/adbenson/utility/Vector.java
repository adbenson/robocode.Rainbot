package net.adbenson.utility;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * The Vector class represents a Mathematical Vector, a measure of Direction and Magnitude. (Not to be confused with an object collection 'Vector')
 * This vector is conceived as being represented by the coordinates of the end of the given vector on a 2-dimensional plane, drawn from the center point of (0, 0) 
 * @author adbenson
 *
 */
public class Vector {
	
	/**
	 * A Neutral (0, 0) vector used for reference
	 */
	public static final Vector NEUTRAL_VECTOR = new Vector();
	
	/**
	 * the X value, or horizontal component of this Vector
	 */
	public final double x;
	/**
	 * the Y value, or vertical component of this Vector
	 */
	public final double y;
	
	/**
	 * Default constructor. Creates a neutral vector at (0, 0)
	 */
	public Vector() {
		x = 0;
		y = 0;
	}
	
	/**
	 * Parameterized constructor. Creates a vector at (x, y)
	 * @param x the horizontal component of this Vector
	 * @param y the vertical component of this Vector
	 */
	public Vector(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Creates a Vector from the given rectangle where (x=width, y=height)
	 * @param rect the rectangle whose dimensions should translate into a Vector
	 */
	public Vector(Rectangle2D rect) {
		this(rect.getWidth(), rect.getHeight());
	}
	
	/**
	 * Creates a new coordinate Vector with the given angle and length (or magnitude) 
	 * @param angle the direction in Radians of the new Vector
	 * @param length the length (or magnitude) of the new new Vector
	 * @return a new coordinate Vector with the given angle and length (or magnitude) 
	 */
	public static Vector getVectorFromAngleAndLength(double angle, double length) {
		double x = Math.sin(angle) * length;
		double y = Math.cos(angle) * length;

		return new Vector(x, y);
	}
	
	/**
	 * Creates a new Vector diametrically opposite this one with the same magnitude.
	 * @return a new Vector diametrically opposite this one with the same magnitude.
	 */
	public Vector invert() {
		return new Vector(-x, -y);
	}

	/**
	 * Returns the angle this vector is pointing to from (0, 0) in Radians
	 * @return the angle this vector is pointing to from (0, 0) in Radians
	 */
	public double getAngle() {
		Vector normal = this.normalize();
		double angle = Math.atan2(normal.x, normal.y);				
		return angle;
	}

	/**
	 * Returns a normalized version (a.k.a. Unit Vector) of this vector, where angle is maintained but magnitude is exactly 1
	 * @return a normalized version (a.k.a. Unit Vector) of this vector, where angle is maintained but magnitude is exactly 1
	 */
	public Vector normalize() {
		double length = this.magnitude();
		//A vector with no length has no direction, either.
		if (length != 0) { 
			return scale(1.0 / length);
		}
		else {
			return NEUTRAL_VECTOR;
		}
	}
	
	/**
	 * Returns the magnitude of this vector, or it's distance / length from (0, 0)
	 * @return the magnitude of this vector, or it's distance / length from (0, 0)
	 */
	public double magnitude() {
		return distance(NEUTRAL_VECTOR);
	}
	
	/**
	 * Returns a new vector of the same orientation where the magnitude is changed by a factor of 'delta'
	 * @param delta the scaling factor for this operation. If delta < 1, magnitude will be reduced. If delta > 1, magnitude will increase
	 * @return a new vector of the same orientation where the magnitude is changed by a factor of 'delta'
	 */
	public Vector scale(double delta) {
		return new Vector(x*delta, y*delta);
	}
	
	/**
	 * Returns a new Vector representing the addition of this and 'addend'.
	 * 	On a graph, this vector would be similar to one vector being drawn starting at the end of the other,
	 *  then creating a vector from (0, 0) to the end of the second vector.
	 * @param addend the Vector to add to this one
	 * @return a new Vector representing the addition of this and 'addend'.
	 */
	public Vector add(Vector addend) {
		return add(addend.x, addend.y);
	}
	
	/**
	 * Returns a new Vector representing the addition of this and the given (x, y) components
	 * 	On a graph, this vector would be similar to one vector being drawn starting at the given (x, y) coordinates,
	 *  then creating a vector from (0, 0) to the end of the vector.
	 * @param x the X component to add to this Vector's X component
	 * @param y the Y component to add to this Vector's Y component
	 * @return a new Vector representing the addition of this and the given (x, y) components
	 */
	public Vector add(double x, double y) {
		return new Vector(this.x + x, this.y + y);
	}
	
	/**
	 * Returns a new Vector representing the subtraction of 'subtrahend' from this.
	 * 	On a graph, this vector would be similar to a vector drawn from then end of this to the end of of 'subtrahend'
	 * @param vector the Vector to substract from this one
	 * @return a new Vector representing the subtraction of 'subtrahend' from this.
	 */
	public Vector subtract(Vector subtrahend) {
		return new Vector(this.x - subtrahend.x, this.y - subtrahend.y);
	}
	
	/**
	 * Returns a new Vector representing this Vector with the addition of another vector of the given 'angle' and 'length'
	 *  See 'add' for a further description
	 * @param angle the angle of the projection from this vector
	 * @param length the length of the projection from this vector
	 * @return a new Vector representing this Vector with the addition of another vector of the given 'angle' and 'length'
	 */
	public Vector project(double angle, double length) {
		return this.add(getVectorFromAngleAndLength(angle, length));
	}

	/**
	 * Returns the distance from the end of this vector to the end of the given Vector
	 * @param other the Vector to get the distance to
	 * @return the distance from the end of this vector to the end of the given Vector
	 */
	public double distance(Vector other) {
		double xDiff = other.x - this.x;
		double yDiff = other.y - this.y;
		
		return Math.sqrt((xDiff * xDiff) + (yDiff * yDiff));
	}
	
	/**
	 * Returns this Vector's X component, rounded to the nearest integer
	 * @return this Vector's X component, rounded to the nearest integer
	 */
	public int intX() {
		return (int) Math.round(x);
	}
	
	/**
	 * Returns this Vector's Y component, rounded to the nearest integer
	 * @return this Vector's Y component, rounded to the nearest integer
	 */
	public int intY() {
		return (int) Math.round(y);
	}

	/**
	 * Returns the distance between this Unit Vector and the given Unit Vector
	 * @param that the Vector to compare
	 * @return the distance between this Unit Vector and the given Unit Vector, between 0 (the same) and 2 (diametrically opposite)
	 */
	public double normalDistance(Vector that) {
		Vector thisNorm = this.normalize();
		Vector thatNorm = that.normalize();
		Vector difference = thisNorm.subtract(thatNorm);
		return difference.magnitude();
	}

	/**
	 * Returns a standard Java {@link Point2D} double-precision representation of this Vector
	 * @return a standard Java {@link Point2D} double-precision representation of this Vector
	 */
	public Point2D toPoint() {
		return new Point2D.Double(x, y);
	}
	
	/**
	 * Draws a line in the given {@link Graphics2D} context, from the end point of this vector along the given direction and for the given length.
	 * @param g the {@link Graphics2D} context to draw to
	 * @param angle the Angle of the line from this Vector
	 * @param length the Length of the line from this Vector
	 */
	public void drawTo(Graphics2D g, double angle, double length) {
		Vector end = this.project(angle, length);
		g.drawLine(intX(), intY(), end.intX(), end.intY());
	}
	
	/**
	 * Returns a String representation of this Vector, including it's X and Y components as well as a unique identifier
	 */
	public String toString() {
		return "Vector: ("+x+", "+y+") [@"+hashCode()+"]";
	}

}