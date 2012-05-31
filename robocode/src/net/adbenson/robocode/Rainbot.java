package net.adbenson.robocode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import net.adbenson.utility.Vector;
import robocode.AdvancedRobot;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.CustomEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;


public class Rainbot extends AdvancedRobot {
	
	public static final double HALF_PI = Math.PI / 2d;
	public static final double MAX_TURN = Math.PI / 5d;
	
	private BattleHistory history;
	
	private TriggerSet triggers;
	
	private float hue;
	
	private static final float HUE_SHIFT = 0.003f;
	private static final int RAINBOW_TURNS = 10;
	
	private boolean rainbowMode;
	private float rainbowTurn;
	
	private Status status;
	
	private BulletQueue bullets;
	private int favoredTurnDirection;
	
	private static Rectangle2D field;
	private double preferredDistance;
	
	public Rainbot() {
		super();
		
		history = new BattleHistory();
		
		triggers = new TriggerSet();
		
		hue = 0;
		
		rainbowMode = false;
			
		status = new Status();
		
		bullets = new BulletQueue();
	
	}
	
	public void run() {
		field = new Rectangle2D.Double(19, 19, getBattleFieldWidth()-38, getBattleFieldHeight()-38);
		preferredDistance = new Vector(field).magnitude() / 2;
		
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
		
	    populateTriggers();
	    
	    triggers.addTo(this);
	    
	    startRadarLock();
	    
	    do {
	    	hueShift();
	        
	    	detectOpponentFire();
	    	
	    	bullets.updateAll(getTime());
	    	history.bullets.updateAll(getTime());
	    	
	    	//Square off!
	    	faceOpponent();
	    	
//	    	setFire(0.3);
//			double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
//			setTurnGunRightRadians(Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians()));
	       	
	    	//Reset all statuses so they will be "clean" for the next round of events
	        status.reset();
	    	execute();

	    } while (true);
	}
	
	private void faceOpponent() {
    	if (!history.isEmpty()) {
    		OpponentState o = history.last.current;
    		double offFace = o.getBearingRadians();
    		//We don't care which direction we face, so treat either direction the same.
    		if (offFace < 0) {
    			offFace += Math.PI;
    		}
    		   		
    		//Offset so that "facing" is 0
    		offFace -= HALF_PI;
    		
    		//Turn farther away the closer we are - by 1/2 field away, straighten out
    		double distanceRatio = Math.max(0, (preferredDistance - o.getDistance()) / (preferredDistance));   		
    		offFace += MAX_TURN * distanceRatio;
    		    		
    		//Multiply the offset - we don't have all day! Move it! (If it's too high, it introduces jitter.)
    		setTurnRight(offFace * 100); 		
    	}
    	else {
    		//Nuthin' better to do...
    		setTurnRight(Double.POSITIVE_INFINITY);
    	}
	}
	
	private void detectOpponentFire() {
		//Check for energy drop, but rule out other causes
		if (status.opponentEnergyDrop &&
				!status.hitToOpponent &&
				!status.collidedWithOpponent) {
		
			//Find the opponent's position on the field
			BattleState opp = history.last;
			Vector oppPos = opp.getPosition(this);

			//Eliminate the possibility of wall crash
			if (!field.contains(oppPos) && opp.stopped()) {
				System.out.println("Looks like he crashed!");
			}
			else {
				System.out.println("Opponent fire detected");
				//Power level of the bullet will be the inverse of the energy drop
				double power = -(history.last.change.getEnergy());
				history.bullets.add(new OpponentBullet(oppPos, opp.current.getBearingRadians(), power, getTime()));
			}
		}
		
	}

	private Vector getPosition() {
		return new Vector(getX(), getY());
	}
	
	public void setFire(double power) {
		bullets.add(new SelfBullet(getPosition(), this.getGunHeadingRadians(), power, getTime()));
		super.setFire(power);
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		history.add(e);
		
		status.opponentEnergyDrop = history.last.change.getEnergy() <= -Rules.MIN_BULLET_POWER;
		
		maintainRadarLock(e);
	}
	
	private void startRadarLock() {
	    setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
	}
	
	private void maintainRadarLock(ScannedRobotEvent e) {
		double radarTurn = getHeadingRadians() + e.getBearingRadians() - getRadarHeadingRadians();
		setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn) * 1.9);
	}
	
	public void onPaint(Graphics2D g) {
		g.setColor(Color.red);
		g.draw(field);
		
		history.bullets.draw(g);
	}
	
	public void onCustomEvent(CustomEvent event) {
		triggers.trigger(event);
	}
	
	public void hueShift() {
		float shift = HUE_SHIFT * (rainbowMode? 3 : 1);
		hue = (hue + shift) % 1.0f;
		setColors(
				Color.getHSBColor(hue, 1, 1),
				Color.getHSBColor(hue + 0.25f, 1, 1),
				Color.getHSBColor(hue + 0.7f, 1, 1),
				Color.orange, //It's just confusing if you can't see the bullet.
				Color.getHSBColor(hue + 0.75f, 1, 1)
		);
	}
	
	private void hueJump() {
		hue = (hue + 0.3f) % 1.0f;
	}
	
	private void rainbow() {
		rainbowMode = true;
		rainbowTurn = 0;
	}
	
	public void onBulletHit(BulletHitEvent event)  {
		rainbow();
		status.hitToOpponent = true;
	}
	
	public void onBulletHitBullet(BulletHitBulletEvent event) {
		//How to detect this...
	}
	
	public void onBulletMissed(BulletMissedEvent event) {
		bullets.removeFirst();
	}
	
	public void onHitByBullet(HitByBulletEvent event) {
		status.hitByOpponent = true;
	}
	
	public void onHitRobot(HitRobotEvent event) {
		status.collidedWithOpponent = true;
	}
	
	public void onHitWall(HitWallEvent event) {
		status.collidedWithWall = true;
	}
	
	public static Rectangle2D getField() {
		return field;
	}
		
	private void populateTriggers() {
		
		triggers.add(new Trigger("b") {
			@Override
			public void action() {
				hueJump();
				rainbowTurn++;
				rainbowMode = (rainbowTurn <= RAINBOW_TURNS);
			}

			@Override
			public boolean test() {
				return rainbowMode;
			}
		});
	}
	
	class Status {
		int bulletCount = 0;
		
		boolean hitByOpponent;
		boolean hitToOpponent;
		boolean collidedWithWall;
		boolean collidedWithOpponent;
		boolean opponentEnergyDrop;
		
		public Status() {
			reset();
		}
		public void reset() {
			hitByOpponent = false;
			hitToOpponent = false;
			collidedWithWall = false;
			collidedWithOpponent = false;
			opponentEnergyDrop = false;
		}
	}
	
}
