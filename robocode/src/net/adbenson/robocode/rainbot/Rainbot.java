package net.adbenson.robocode.rainbot;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import net.adbenson.robocode.botstate.BattleHistory;
import net.adbenson.robocode.botstate.BotState.StateMatchComparator;
import net.adbenson.robocode.botstate.OpponentState;
import net.adbenson.robocode.botstate.OpponentState.PredictiveStateUnavailableException;
import net.adbenson.robocode.bullet.Bullet;
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
	
	public static final int PREDICTIVE_LOOKBEHIND = 30;
	private StateMatchComparator<OpponentState> predictiveComparator;
	
	private LinkedList<OpponentState> opponentPrediction;
	
	private OpponentState candidateTarget;
	
	private boolean ready;
	private boolean aim;
	
	private double TARGET_FIREPOWER = 1.5;
	
	private double requiredFirepower;
	
	public Rainbot() {
		super();
		
		history = new BattleHistory();
		
		triggers = new TriggerSet();
		
		hue = 0;
		
		rainbowMode = false;
			
		status = new Status();	
		
		preferredDirection = -1;
		
		predictiveComparator = new HeadingVelocityStateComparator();
		
		ready = false;
		aim = false;
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
	    	
//			double velocityTrend = Math.abs(o.previous.change.velocity - o.change.velocity);
//			double headingTrend = Math.abs(o.previous.change.heading - o.change.heading);
	    	
	    	if (history.size() > PREDICTIVE_LOOKBEHIND) {
//	    		if (velocityTrend < 0.01 && headingTrend < 0.01 && o.change.heading < 0.01) {

		    	//ONLY look into prediction if we're not preparing to fire or have just fired 
	    		if (!ready && this.getGunHeat() <= getGunCoolingRate()) {
		    		opponentPrediction = predictTheFuture();
	    			
		    		ready = false;
	    			aim = false;
		    		
		    		if (opponentPrediction != null) {
		    			
						try {
							Target target = getBearingFromPrediction(opponentPrediction, TARGET_FIREPOWER);
							
							requiredFirepower = target.power;
							
			    			this.setTurnGunRightRadians(
			    					Utility.angleDifference(target.bearing, this.getGunHeadingRadians()));
			    			
			    			ready = true;
						} catch (UnableToTargetPredictionException e) {
							System.out.println("Predicted target unreachable");
						}
		    		}
	    		}
		    	
		    	if (ready && this.getGunTurnRemainingRadians() <= Rules.GUN_TURN_RATE_RADIANS) {	    		
		    		aim = true;
		    	}
		    	
		    	if (ready && aim) {
		    		setFire(requiredFirepower);   		
		    		ready = false;
		    		aim = false;
		    	}
	    	
	    	}
		       	
	    	//Reset all statuses so they will be "clean" for the next round of events
	        status.reset();
	    	execute();

	    } while (true);
	}
	
	private Target getBearingFromPrediction(LinkedList<OpponentState> prediction, double targetPower) throws UnableToTargetPredictionException {	
		Vector position = getPosition();						
		int turnsToPosition = 0;
		
		TreeMap<Double, OpponentState> potentialTargets = new TreeMap<Double, OpponentState>();
		
		for(OpponentState target : prediction) {
			turnsToPosition++;
						
			double distance = target.position.distance(position);
			double requiredPower = Bullet.getRequiredPower(turnsToPosition, distance);
			
			//If the power required is below the minimum, it can't possibly get there in time.
			if (requiredPower >= Rules.MIN_BULLET_POWER) {
				potentialTargets.put(requiredPower, target);				
			}
		}
		
		//If nothing is added to the list, nothing is in range.
		if (potentialTargets.isEmpty()) {
			throw new UnableToTargetPredictionException();
		}
		
		//Get the closest entry with a required power >= targetPower
		Map.Entry<Double, OpponentState> closestMatch = 
			potentialTargets.ceilingEntry(targetPower);
		
		//If there's none, just take the highest available.
		if (closestMatch == null) {
			closestMatch = potentialTargets.lastEntry();
		}
		
		double power = closestMatch.getKey();
		candidateTarget = closestMatch.getValue();
		Vector offset = candidateTarget.position.subtract(position);
		
		return new Target(power, offset.getAngle());
	}

	private LinkedList<OpponentState> predictTheFuture() {
		OpponentState o = history.getCurrentOpponent();
		LinkedList<OpponentState> prediction = null;

		long start = System.nanoTime();
		
		OpponentState bestMatch = o.matchStateSequence(PREDICTIVE_LOOKBEHIND, predictiveComparator);
		
		if (bestMatch != null) {
    		try {
    			prediction = o.predictStates(bestMatch, PREDICTIVE_LOOKBEHIND);
			} catch (PredictiveStateUnavailableException e) {
				System.out.println("Prediction failed due to unavailable data");
			}
		}
		
		System.out.print("time:"); System.out.format("%,8d", System.nanoTime() - start);
		System.out.println(" ("+getTime()+")");

		return prediction;
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
		history.selfFired(candidateTarget);
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
		
//		g.setColor(Color.red);
//		g.draw(field);
//		
//		g.setColor(Color.green);
//		g.draw(safety);
		
		if (!history.isEmpty()) {
			
			g.setStroke(new BasicStroke(3));
			
			history.getOpponentBullets().draw(g);
			history.getSelfBullets().draw(g);
			
			history.getCurrentState().opponent.draw(g);
			
			history.getCurrentState().self.draw(g);
		}
		
		if (opponentPrediction != null) { 
			for(int i=0; i<opponentPrediction.size(); i++) {
				opponentPrediction.get(i).drawPath(g, i);
			}
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

	private class UnableToTargetPredictionException extends Exception {}
	
	private class Target {
		public final double power;
		public final double bearing;
		public Target(double power, double bearing) {
			this.power = power;
			this.bearing = bearing;
		}
	}
	
}
