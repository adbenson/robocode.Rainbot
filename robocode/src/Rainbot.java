import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import robocode.AdvancedRobot;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.CustomEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;


public class Rainbot extends AdvancedRobot {
	
	public static final double HALF_PI = Math.PI / 2d;
	
	private OpponentHistory opponentHistory;
	
	private TriggerSet triggers;
	
	private float hue;
	
	private static final float HUE_SHIFT = 0.003f;
	private static final int RAINBOW_TURNS = 10;
	
	private boolean rainbowMode;
	private float rainbowTurn;
	
	private Status status;
	
	private BulletQueue bullets;
	
	private Rectangle2D field;
	
	public Rainbot() {
		super();
		
		opponentHistory = new OpponentHistory();
		
		triggers = new TriggerSet();
		
		hue = 0;
		
		rainbowMode = false;
			
		status = new Status();
	
	}
	
	public void run() {
		field = new Rectangle2D.Double(19, 19, getBattleFieldWidth()-38, getBattleFieldHeight()-38);
		
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
		
	    populateTriggers();
	    
	    triggers.addTo(this);
	    
	    startRadarLock();
	    
	    do {
	    	hueShift();
	        
	    	detectOpponentFire();
	    	
	    	//Square off
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
    	if (!opponentHistory.isEmpty()) {
    		Opponent o = opponentHistory.getLast();
    		double offFace = o.getBearingRadians();
    		//We don't care which direction we face, so treat either direction the same.
    		if (offFace < 0) {
    			offFace += Math.PI;
    		}
    		
    		//Offset so that "facing" is 0
    		offFace -= HALF_PI;
    		
    		//Multiply the offset - we don't have all day! Move it!
    		//(If it's too high, it introduces jitter.
    		setTurnRight(offFace * 100); 		
    	}
    	else {
    		//Nothin' better to do...
    		setTurnRight(Double.POSITIVE_INFINITY);//opponentHistory.getLast().getBearingRadians() + HALF_PI);
    	}
	}
	
	private void detectOpponentFire() {
		//Check for energy drop, but rule out other causes
		if (status.opponentEnergyDrop &&
				!status.hitToOpponent &&
				!status.collidedWithOpponent) {
		
			//Find the opponent's position on the field
			Opponent opp = opponentHistory.getLast();
			Point2D oppPos = opp.getAbsolutePosition(this);
			
			//Eliminate the possibility of wall crash
			if (!field.contains(oppPos)) {
				System.out.println("Looks like he crashed!");
			}
			else {
				System.out.println("Opponent fire detected");
				//Power level of the bullet will be the inverse of the energy drop
				double power = -(opponentHistory.lastChange.getEnergy());
				opponentHistory.bullets.add(new OpponentBullet(oppPos, power, getTime()));
			}
		}
		
	}

	private Point2D getPosition() {
		return new Point2D.Double(getX(), getY());
	}
	
	public void setFire(double power) {
		bullets.add(new SelfBullet(getPosition(), this.getGunHeadingRadians(), power, getTime()));
		super.setFire(power);
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		opponentHistory.add(e);

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
		status.nextCollidedWithOpponent = true;
	}
	
	public void onHitWall(HitWallEvent event) {
		status.collidedWithWall = true;
	}
		
	private void populateTriggers() {
		System.out.println("Creating triggers");
		
		triggers.add(new Trigger("Opponent Fired") {
			@Override
			public void action() {			
				status.opponentEnergyDrop = true;
			}

			@Override
			public boolean test() {
				if (opponentHistory.lastChange != null) {
					return (opponentHistory.lastChange.getEnergy() <= -0.1);
				}
				else {
					return false;
				}
			}
		});
		
		
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
		boolean nextCollidedWithOpponent;
		boolean opponentEnergyDrop;
		
		public Status() {
			reset();
		}
		public void reset() {
			hitByOpponent = false;
			hitToOpponent = false;
			collidedWithWall = false;
			collidedWithOpponent = nextCollidedWithOpponent;
			nextCollidedWithOpponent = false;
			opponentEnergyDrop = false;
		}
	}
	
}
