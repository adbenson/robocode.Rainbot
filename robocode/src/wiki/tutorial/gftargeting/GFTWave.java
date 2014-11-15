package wiki.tutorial.gftargeting;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Random;

import robocode.Condition;
import robocode.util.Utils;

class GFTWave extends Condition {
	static Random rand = new Random();
	
	static Point2D targetLocation;

	private static final double MAX_DISTANCE = 900;
	private static final int DISTANCE_INDEXES = 5;
	private static final int VELOCITY_INDEXES = 5;
	private static final int BINS = 25;
	private static final int LAST_BIN = BINS - 1;
	private static final int MIDDLE_BIN = LAST_BIN / 2;
	private static final double MAX_ESCAPE_ANGLE = 0.7;
	private static final double BIN_WIDTH = MAX_ESCAPE_ANGLE / (double)MIDDLE_BIN;
	
	double bulletPower;
	Point2D gunLocation;
	double bearing;
	double lateralDirection;
	
	private static int[][][][] statBuffers = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES][BINS];

	private int[] buffer;
	private double distanceTraveled;
	
	private Color color;
	
	GFTWave() {
		color = Color.getHSBColor(rand.nextFloat(), 1, 1);
	}
	
	public boolean test() {
		advance();
		if (hasArrived()) {
			buffer[currentBin()]++;
//			robot.removeCustomEvent(this);
		}
		return false;
	}

	double mostVisitedBearingOffset() {
		return (lateralDirection * BIN_WIDTH) * (mostVisitedBin() - MIDDLE_BIN);
	}
	
	void setSegmentations(double distance, double velocity, double lastVelocity) {
		int distanceIndex = (int)(distance / (MAX_DISTANCE / DISTANCE_INDEXES));
		int velocityIndex = (int)Math.abs(velocity / 2);
		int lastVelocityIndex = (int)Math.abs(lastVelocity / 2);
		buffer = statBuffers[distanceIndex][velocityIndex][lastVelocityIndex];
	}

	private void advance() {
		distanceTraveled += GFTUtils.bulletVelocity(bulletPower);
	}

	private boolean hasArrived() {
		return distanceTraveled > gunLocation.distance(targetLocation) - 18;
	}
	
	private int currentBin() {
		double absBearing = GFTUtils.absoluteBearing(gunLocation, targetLocation);
		double normalBearing = Utils.normalRelativeAngle(absBearing - bearing);
		double binOffset = normalBearing / (lateralDirection * BIN_WIDTH);
		
		int bin = (int)Math.round(binOffset + MIDDLE_BIN);
		return GFTUtils.minMax(bin, 0, LAST_BIN);
	}
	
	private int mostVisitedBin() {
		int mostVisited = MIDDLE_BIN;
		for (int i = 0; i < BINS; i++) {
			if (buffer[i] > buffer[mostVisited]) {
				mostVisited = i;
			}
		}
		return mostVisited;
	}

	public void onPaint(Graphics2D g) {
		g.setColor(color);
		
		g.drawOval((int)gunLocation.getX(), (int)gunLocation.getY(), (int)distanceTraveled, (int)distanceTraveled);
	}
}