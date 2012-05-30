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
	    
	    setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
	    
	    do {
	    	hueShift();
	        
	    	detectOpponentFire();
	    	
//	    	setFire(0.3);
	    	
//			double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
//			setTurnGunRightRadians(Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians()));
	       	        
	        status.reset();
	    	execute();

	    } while (true);
	}
	
	private void detectOpponentFire() {
		if (status.opponentEnergyDrop &&
				!status.hitToOpponent &&
				!status.collidedWithOpponent) {
		
			Point2D oppPos = opponentHistory.getLast().getAbsolutePosition(this);
			if (!field.contains(oppPos)) {
				System.out.println("Looks like he crashed!");
			}
			else {
				System.out.println("Opponent fire detected");
			}
		}
		
	}

	private Point2D getPosition() {
		return new Point2D.Double(getX(), getY());
	}

	public void fire(double power) {
		setFire(power);
		bullets.add(new Bullet(getX(), getY(), this.getGunHeadingRadians(), power, getTime()));
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		opponentHistory.add(e);

		double radarTurn = getHeadingRadians() + e.getBearingRadians() - getRadarHeadingRadians();
		setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn) * 1.9);
	}
	
	public void onPaint(Graphics2D g) {

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
