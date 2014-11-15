package wiki.tutorial.gftargeting;

import java.awt.geom.Point2D;

import robocode.AdvancedRobot;
import robocode.Bullet;
import robocode.Condition;
import robocode.util.Utils;

class Wave {
	
	private Bullet bullet;

	private Point2D origin;
	private double bearing;
	private double lateralDirection;

	private double distanceTraveled;
	
	private double distancePerTick;
	
	private int[] buffer;
	
//	Wave(Point2D gunLocation, double bulletPower, double bearing, double lateralDirection) {
//		distancePerTick = GFTUtils.bulletVelocity(bulletPower);
//	}
	
	public Wave(Bullet bullet) {
		this.bullet = bullet;
		
		this.bearing = bullet.getHeading();
		this.origin = new Point2D.Double(bullet.getX(), bullet.getY());
	}
	
	public void advance() {
		distanceTraveled += distancePerTick;
	}

	public boolean hasArrived(Point2D targetLocation) {
		return !bullet.isActive() || distanceTraveled > origin.distance(targetLocation) - 18;
	}
	
	private int currentBin() {
		int bin = (int)Math.round(((Utils.normalRelativeAngle(GFTUtils.absoluteBearing(origin, targetLocation) - bearing)) /
				(lateralDirection * WaveManager.BIN_WIDTH)) + WaveManager.MIDDLE_BIN);
		return GFTUtils.minMax(bin, 0, WaveManager.BINS - 1);
	}
	
	public void setSegmentations(double distance, double velocity, double lastVelocity) {
		int distanceIndex = (int)(distance / ( WaveManager.MAX_DISTANCE /  WaveManager.DISTANCE_INDEXES));
		int velocityIndex = (int)Math.abs(velocity / 2);
		int lastVelocityIndex = (int)Math.abs(lastVelocity / 2);
		buffer = statBuffers[distanceIndex][velocityIndex][lastVelocityIndex];
	}

}