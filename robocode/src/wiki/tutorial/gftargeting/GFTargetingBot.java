package wiki.tutorial.gftargeting;
import java.awt.Color;
import java.awt.geom.Point2D;

import net.adbenson.robocode.TurnBot;
import robocode.Bullet;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
//
// GFTargetingBot, by PEZ. A simple GuessFactorTargeting bot for tutorial purposes.

public class GFTargetingBot extends TurnBot {
	private static final double BULLET_POWER = 1.9;
	
	private double lateralDirection;
	private double lastEnemyVelocity;
	private GFTMovement movement;
	
	private WaveManager waves;
	
	public GFTargetingBot() {
		movement = new GFTMovement(this);
		waves = new WaveManager();
	}
	
	public void init() {
		setColors(Color.BLUE, Color.BLACK, Color.YELLOW);
		lateralDirection = 1;
		lastEnemyVelocity = 0;
		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);
	}
	
	public void turn() {
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY); 
		
		waves.advanceAll();
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		double enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double enemyVelocity = e.getVelocity();
		
		if (enemyVelocity != 0) {
			lateralDirection = GFTUtils.sign(enemyVelocity * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing));
		}
		
		lastEnemyVelocity = enemyVelocity;
		
		if (getEnergy() >= BULLET_POWER) {
			aimAndFire(enemyAbsoluteBearing, enemyVelocity);
		}
		
		movement.onScannedRobot(e);
		setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
	}

	private void aimAndFire(double enemyAbsoluteBearing, double enemyVelocity) {
		double enemyDistance = e.getDistance();
		
		Point2D gunLocation = new Point2D.Double(getX(), getY());
		Point2D targetLocation = GFTUtils.project(gunLocation, enemyAbsoluteBearing, enemyDistance);

		setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getGunHeadingRadians() + wave.mostVisitedBearingOffset()));
		Bullet bullet = setFireBullet(BULLET_POWER);
		
		Wave wave = new Wave(bullet);
		wave.setSegmentations(enemyDistance, enemyVelocity, lastEnemyVelocity);
		
		waves.add(wave);
	}
	
}