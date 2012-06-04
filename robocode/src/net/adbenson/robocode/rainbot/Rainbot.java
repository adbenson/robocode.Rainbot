package net.adbenson.robocode.rainbot;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Comparator;

import net.adbenson.robocode.botstate.BattleHistory;
import net.adbenson.robocode.botstate.BotState.StateComparisonUnavailableException;
import net.adbenson.robocode.botstate.BotState.StateMatchComparator;
import net.adbenson.robocode.botstate.OpponentState;
import net.adbenson.robocode.trigger.Trigger;
import net.adbenson.robocode.trigger.TriggerSet;
import net.adbenson.utility.Utility;
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
	
	public static final double MAX_TURN = Math.PI / 5d;
	
	private BattleHistory history;
	
	private TriggerSet triggers;
	
	private float hue;
	
	private static final float HUE_SHIFT = 0.003f;
	private static final int RAINBOW_TURNS = 10;
	
	private boolean rainbowMode;
	private float rainbowTurn;
	
	private Status status;
	
	private int preferredDirection;
	
	private static Rectangle2D field;
	private Rectangle2D safety;
	private double preferredDistance;
	
	private static final int PREDICTIVE_LOOKBEHIND = 30;
	private StateMatchComparator<OpponentState> predictiveComparator;
	
	public Rainbot() {
		super();
		
		history = new BattleHistory();
		
		triggers = new TriggerSet();
		
		hue = 0;
		
		rainbowMode = false;
			
		status = new Status();	
		
		preferredDirection = -1;
		
		predictiveComparator = new HeadingVelocityStateComparator();
	}
	
	public void run() {
		Vector botSize = new Vector(getWidth(), getHeight());
		
		field = new Rectangle2D.Double(
				(botSize.x/2)+1, (botSize.y/2)+1, 
				getBattleFieldWidth()-(botSize.x-2), getBattleFieldHeight()-(botSize.y-2)
		);
		safety = new Rectangle2D.Double(
				botSize.x, botSize.y, 
				getBattleFieldWidth()-(botSize.x*2), getBattleFieldHeight()-(botSize.y*2)
		);
		
		preferredDistance = new Vector(field).magnitude() / 2;
		
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
		
	    populateTriggers();
	    
	    triggers.addTo(this);
	    
	    startRadarLock();
	    
	    do {
	    	hueShift();
	    	
	    	//Square off!
	    	faceOpponent();
	        
	    	detectOpponentFire();
	    		    	
	    	history.getSelfBullets().updateAll(getTime());
	    	history.getOpponentBullets().updateAll(getTime());
	    	
	    	if (history.size() > PREDICTIVE_LOOKBEHIND) {
	    		OpponentState o = history.getCurrentOpponent();
	    		
	    		
	    		long start = System.nanoTime();
	    		
	    		OpponentState bestMatch = o.matchStateSequence(PREDICTIVE_LOOKBEHIND, predictiveComparator);
	    		
	    		System.out.print("time:");
	    		System.out.format("%,8d", System.nanoTime() - start);
	    		System.out.println(" ("+history.size()+")");


	    	}
		       	
	    	//Reset all statuses so they will be "clean" for the next round of events
	        status.reset();
	    	execute();

	    } while (true);
	}
	
	private void faceOpponent() {
    	if (!history.isEmpty()) {
    		OpponentState o = history.getCurrentOpponent();
    		double offFace = o.bearing;
    		//We don't care which direction we face, so treat either direction the same.
    		if (offFace < 0) {
    			offFace += Math.PI;
    		}
    		   		
    		//Offset so that "facing" is 0
    		offFace -= Utility.HALF_PI;
    		
    		//Turn farther away the closer we are - by 1/2 field away, straighten out
    		double distanceRatio = (preferredDistance - o.distance) / (preferredDistance);   		
    		offFace += MAX_TURN * distanceRatio * preferredDirection;
    		    		
    		//Multiply the offset - we don't have all day! Move it! (If it's too high, it introduces jitter.)
    		setTurnRight(offFace * 10); 
    		
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
			OpponentState opponent = history.getCurrentOpponent();
			Vector opponentPos = opponent.getPosition();

			//Eliminate the possibility of wall crash
			if (!field.contains(opponentPos.toPoint()) && opponent.stopped()) {
				System.out.println("Looks like he crashed!");
			}
			else {
				System.out.println("Opponent fire detected");
				history.opponentFired();
				preferredDirection = -preferredDirection;
				setAhead(100 * preferredDirection);
			}
		}
		
	}

	private Vector getPosition() {
		return new Vector(getX(), getY());
	}
	
	public void setFire(double power) {
		history.selfFired();
		super.setFire(power);
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		history.addBots(this, e, getTime());
		
		if (history.getCurrentOpponent().change != null) {
			status.opponentEnergyDrop = history.getCurrentOpponent().change.energy <= -Rules.MIN_BULLET_POWER;
		}
		
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
		if (!history.isEmpty()) {
			g.setColor(Color.red);
			g.draw(field);
			
			g.setColor(Color.green);
			g.draw(safety);
			
			g.setStroke(new BasicStroke(3));
			
			history.getOpponentBullets().draw(g);
			
			history.getCurrentState().opponent.draw(g);
			
			history.getCurrentState().self.draw(g);
		}
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
		history.getSelfBullets().removeFirst();
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
