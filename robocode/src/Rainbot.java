import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

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
	
	public Rainbot() {
		super();
		
		opponentHistory = new OpponentHistory();
		
		triggers = new TriggerSet();
		
		hue = 0;
		
		rainbowMode = false;
	
		
		status = new Status();
	}
	
	public void run() {
	    populateTriggers();
	    
	    triggers.addTo(this);
	    
	    setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
	    
	    do {
	    	hueShift();
	    	
	        setFire(0.3);
	       	        
	        status.reset();
	        
	    	execute();
	    } while (true);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
//		System.out.println("Robot Scanned: "+e.getName());
		opponentHistory.add(e);
		
	    double radarTurn =
	            // Absolute bearing to target
	            getHeadingRadians() + e.getBearingRadians()
	            // Subtract current radar heading to get turn required
	            - getRadarHeadingRadians();
	     
	    setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn) * 1.9);   
//	    
//		
//      double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
//      setTurnGunRightRadians(Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians()));
	}
	
	public void onPaint(Graphics2D g) {

	}
	
	public AdvancedRobot getThisBot() {
		return this;
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
	}
	
	public void onBulletHitBullet(BulletHitBulletEvent event) {
		status.bulletCount--;
	}
	
	public void onBulletMissed(BulletMissedEvent event) {
		status.bulletCount--;
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
		
	private void populateTriggers() {
		System.out.println("Creating triggers");
		
		triggers.add(new Trigger("Opponent Fired") {
			@Override
			public void action() {
				System.out.println("Opponent Shot Detected...");
				hueJump();
			}

			@Override
			public boolean test() {
				if (opponentHistory.lastChange != null) {
					Opponent change = opponentHistory.lastChange;			
					//A drop in energy greater that 0.1 is probably a shot
					//Elimiate other likely possibilities
					if (status.collidedWithOpponent || status.hitOpponent) {
						return false;
					}
					else {
						return (change.getEnergy() <= -0.1);
					}
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
		boolean hitOpponent;
		boolean collidedWithWall;
		boolean collidedWithOpponent;
		
		public Status() {
			reset();
		}
		public void reset() {
			hitByOpponent = false;
			hitOpponent = false;
			collidedWithWall = false;
			collidedWithOpponent = false;
		}
	}
	
}
