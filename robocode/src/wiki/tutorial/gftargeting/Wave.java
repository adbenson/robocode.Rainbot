package wiki.tutorial.gftargeting;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Random;

import robocode.Bullet;
import robocode.Robocode;
import robocode.util.Utils;

public class Wave {
	private static Random rand = new Random();
	private static final int HALF_BOT = TargetingBot.BOT_SIZE / 2;
	
	final Point2D origin;
	final double bearing;
	final double heading;
	final Bullet bullet;
	
	private final double distancePerTick;
	
	private final Color color;
	
	private double distanceTraveled;
	private boolean past;

	public Wave(Bullet bullet, double bearing) {
		
		distanceTraveled = 0;
		distancePerTick = TargetingUtils.bulletTravelPerTick(bullet.getPower());
		
		this.bearing = bearing;
		this.heading = bullet.getHeadingRadians();
		this.origin = new Point2D.Double(bullet.getX(), bullet.getY());
		this.bullet = bullet;
		
		color = Color.getHSBColor(rand.nextFloat(), 1, 1);
	}

	public void advance() {
		distanceTraveled = getDistanceTraveled() + distancePerTick;
	}

	public boolean hasArrived(Point2D targetLocation) {
		return getDistanceTraveled() > origin.distance(targetLocation) - 18;
	}
	
	public int angleOffsetIndex(Point2D targetLocation, int opponentDirection) {
		double absBearing = TargetingUtils.absoluteBearing(origin, targetLocation);
		
		double normalBearing = Utils.normalRelativeAngle(absBearing - bearing);
		double binOffset = normalBearing / (opponentDirection * WaveManager.BIN_WIDTH);
		
		int bin = (int)Math.round(binOffset + WaveManager.MIDDLE_BIN);
		return TargetingUtils.minMax(bin, 0, WaveManager.LAST_BIN);
	}
	
	public void draw(Graphics2D g) {
		g.setColor(color);
		
		int radius = (int)getDistanceTraveled();
		int diameter = radius * 2;
		g.drawOval((int)origin.getX() - radius, (int)origin.getY() - radius, diameter, diameter);
		
		Point2D end = TargetingUtils.project(origin, heading, getDistanceTraveled());
		g.drawLine((int)origin.getX(), (int)origin.getY(),(int)end.getX(), (int)end.getY());
	}

	public double getDistanceTraveled() {
		return distanceTraveled;
	}

}