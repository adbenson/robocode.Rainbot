package wiki.tutorial.gftargeting;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import net.adbenson.robocode.TurnBot;
import robocode.Bullet;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

/**
 * TargetingBot
 * @author welah
 *
 * Based on GFTargetingBot, by PEZ. A simple GuessFactorTargeting bot for tutorial purposes.
 *
 * This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
 * http://robowiki.net/?RWPCL
 */

public class TargetingBot extends TurnBot {
	private static final double BULLET_POWER = 1.9;
	public static final int BOT_SIZE = 36;
	
	private double enemyBearing;
	private double enemyDistance;
	private Point2D enemyLocation;
	private int enemyRelativeDirection;
	
	private BasicMovement movement;
	
	private WaveManager waves;
	
	public TargetingBot() {
		movement = new BasicMovement(this);
		waves = new WaveManager();
	}
	
	public void init() {
		setColors(Color.BLUE, Color.BLACK, Color.YELLOW);

		enemyRelativeDirection = 1;
		
		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);
		
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY); 
	}
	
	public void turn() {
		
		waves.advanceAll(enemyLocation, enemyRelativeDirection);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		
		//Note that bearing from the scan even is relative to our heading, so add that to make it absolute.
		enemyBearing = getHeadingRadians() + e.getBearingRadians();
		enemyDistance = e.getDistance();
		
		enemyLocation = TargetingUtils.project(location(), enemyBearing, enemyDistance);
		
		//If enemy velocity is zero, we can't calculate a new direction
		//so let the previous direction stand
		if (e.getVelocity() != 0) {
			double direction = Math.sin(e.getHeadingRadians() - enemyBearing);
			enemyRelativeDirection = (int) Math.signum(e.getVelocity() * direction);
		}
		
		//Turn to the ideal firing angle, even if we're not firing so we aren't trying to switch quickly.
		setFiringAngle();

		//Don't bother unless we can actually fire
		if (getEnergy() >= BULLET_POWER && getGunHeat() <= 0) {
			shoot();
		}
		
		//Basic bullet dodging
		movement.onScannedRobot(e);
		
		//Maintain radar lock
		setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyBearing - getRadarHeadingRadians()) * 2);
	}

	private void setFiringAngle() {
		double firingAngle = enemyBearing - getGunHeadingRadians() + waves.mostVisitedBearingOffset(enemyRelativeDirection);
		setTurnGunRightRadians(Utils.normalRelativeAngle(firingAngle));
	}

	private void shoot() {
		
		Bullet bullet = setFireBullet(BULLET_POWER);
		Wave wave = new Wave(bullet, enemyBearing);
		
		waves.add(wave);
	}
	
	public Point2D location() {
		return new Point2D.Double(getX(), getY());
	}
	
	@Override
	public void onPaint(Graphics2D g) {
		waves.drawAll(g, location());
		
		waves.drawEscapeSegments(g, location(), enemyBearing, enemyDistance);
		if (enemyLocation != null) {
//			waves.drawEscapeMetrics(g, location(), enemyBearing, enemyDistance, enemyLocation);
		}
	}
	
}