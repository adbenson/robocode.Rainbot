package net.adbenson.robocode.rainbot;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.adbenson.robocode.botstate.BattleHistory;
import net.adbenson.robocode.botstate.BotState.StateMatchComparator;
import net.adbenson.robocode.botstate.OpponentState;
import net.adbenson.robocode.botstate.OpponentState.PredictiveStateUnavailableException;
import net.adbenson.robocode.bullet.Bullet;
import net.adbenson.robocode.prediction.HeadingVelocityStateComparator;
import net.adbenson.robocode.prediction.ImpossibleToSeeTheFutureIsException;
import net.adbenson.utility.Utility;
import net.adbenson.utility.Vector;
import robocode.AdvancedRobot;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.RobotDeathEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;


public class Rainbot extends AdvancedRobot {
	
	public static final int PREDICTIVE_LOOKBEHIND = 100;
	public static final double TARGET_FIREPOWER = 1;
	
	public static final double MAX_TURN = Math.PI / 5d;

	
	private BattleHistory history;
	
	private RoundStatus status;
	
	private int preferredDirection;
	
	private static Rectangle2D field;
	private Rectangle2D safety;
	
	private double preferredDistance;

	private StateMatchComparator<OpponentState> predictiveComparator;
	
	private LinkedList<OpponentState> opponentPrediction;
	private OpponentState candidateTarget;
	
	private boolean opponentAlive;
	
	private BotColor color;
	
	public Rainbot() {
		super();
		
		history = new BattleHistory();
			
		status = new RoundStatus();	
		
		preferredDirection = 1;
		
		predictiveComparator = new HeadingVelocityStateComparator();
		
		color = new BotColor();
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
		
		opponentAlive = true;
		
		preferredDistance = new Vector(field).magnitude() / 2;
		
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
	    
	    startRadarLock();
	    
	    double requiredFirepower = 0;
		boolean ready = false;
		boolean aim = false;
		boolean fire = false;
	    
	    do {
	    	color.hueShift(this);
	    	
	    	//Square off!
	    	faceOpponent();
	        
	    	detectOpponentFire();
	    		    	
	    	history.getSelfBullets().updateAll(getTime());
	    	history.getOpponentBullets().updateAll(getTime());
	    	
//			double velocityTrend = Math.abs(o.previous.change.velocity - o.change.velocity);
//			double headingTrend = Math.abs(o.previous.change.heading - o.change.heading);
	    	
	    	if (getTime() > PREDICTIVE_LOOKBEHIND) {
//	    		if (velocityTrend < 0.01 && headingTrend < 0.01 && o.change.heading < 0.01) {
	    		
		    	if (ready && Math.abs(this.getGunTurnRemainingRadians()) < Rules.GUN_TURN_RATE_RADIANS) {	    		
		    		aim = true;
		    		fire = false;
		    	}
	    		
		    	if (opponentAlive && ready && aim && !fire) {
		    		setFire(requiredFirepower);   		
		    		ready = false;
		    		aim = false;
		    		fire = true;
		    	}


		    	//ONLY look into prediction if we're not preparing to fire or have recently fired 
	    		if (!ready && this.getGunHeat() <= getGunCoolingRate()) {
	    			
					try {
						opponentPrediction = predictTheFuture();

						Entry<Double, OpponentState> target = selectTargetFromPrediction(
								opponentPrediction, TARGET_FIREPOWER);

						requiredFirepower = target.getKey();
						setGunTurnToTarget(target.getValue());

						ready = true;

					} catch (UnableToTargetPredictionException e) {
						System.out.println("Predicted target unreachable");
					} catch (PredictiveStateUnavailableException e) {
						System.out.println("Not enough history or future to provide prediction");
					} catch (ImpossibleToSeeTheFutureIsException e) {
						System.out.println("Impossible to see, the future is.");
					}
		    		
	    			aim = false;
	    			fire = false;
	    		}   	

	    	
	    	}
		       	
	    	//Reset all statuses so they will be "clean" for the next round of events
	        status.reset();
	    	execute();

	    } while (true);
	}
	
	private void setGunTurnToTarget(OpponentState target) {
		Vector offset = target.position.subtract(getPosition());
		double heading = Utility.angleDifference(offset.getAngle(), this.getGunHeadingRadians());
		this.setTurnGunRightRadians(heading);
	}
	
	private Map.Entry<Double, OpponentState> selectTargetFromPrediction(LinkedList<OpponentState> prediction, double targetPower) throws UnableToTargetPredictionException {	
		Vector position = getPosition();						
		int turnsToPosition = 0;
		
		TreeMap<Double, OpponentState> potentialTargets = new TreeMap<Double, OpponentState>();
		
		for(OpponentState target : prediction) {
			turnsToPosition++;
						
			double distance = target.position.distance(position);
			double requiredPower = Bullet.getRequiredPower(turnsToPosition, distance);
		
			//If the power required is below the minimum, it can't possibly get there in time.
			if (requiredPower >= Rules.MIN_BULLET_POWER && requiredPower <= Rules.MAX_BULLET_POWER) {
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
		
		//Store this so we can draw it later
		candidateTarget = closestMatch.getValue();
		
		return closestMatch;
	}

	private LinkedList<OpponentState> predictTheFuture() throws PredictiveStateUnavailableException, ImpossibleToSeeTheFutureIsException {
		OpponentState o = history.getCurrentOpponent();
		LinkedList<OpponentState> prediction = null;

		long start = System.nanoTime();
		
		OpponentState bestMatch = o.matchStateSequence(PREDICTIVE_LOOKBEHIND, predictiveComparator);
		
		if (bestMatch != null) {
    		prediction = o.predictStates(bestMatch, PREDICTIVE_LOOKBEHIND);
		}
		else {
			throw new ImpossibleToSeeTheFutureIsException();
		}
		
		System.out.print("time:"); System.out.format("%,8d", System.nanoTime() - start);
		System.out.println(" ("+getTime()+")");

		return prediction;
	}
	
	private void faceOpponent() {
    	if (history.hasCurrentState()) {
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
System.out.println("Firing@"+power);		
		robocode.Bullet bullet = super.setFireBullet(power);
		history.selfFired(candidateTarget, bullet);
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		history.addBots(this, e, getTime());
		
		if (history.getCurrentOpponent().change != null) {
			status.opponentEnergyDrop = history.getCurrentOpponent().change.energy <= -Rules.MIN_BULLET_POWER;
		}
		
		maintainRadarLock(e);
	}
	
	public void onRobotDeath(RobotDeathEvent event) {
		opponentAlive = false;
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
		
		if (history.hasCurrentState()) {
			
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
		
	public void onBulletHit(BulletHitEvent event)  {
		color.startRainbow();
		status.hitToOpponent = true;
	}
	
	public void onBulletHitBullet(BulletHitBulletEvent event) {
		history.getSelfBullets().remove(event.getBullet());
		history.getOpponentBullets().remove(event.getHitBullet());
	}
	
	public void onBulletMissed(BulletMissedEvent event) {
		history.getSelfBullets().remove(event.getBullet());
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
			
	class RoundStatus {
		int bulletCount = 0;
		
		boolean hitByOpponent;
		boolean hitToOpponent;
		boolean collidedWithWall;
		boolean collidedWithOpponent;
		boolean opponentEnergyDrop;
		
		public RoundStatus() {
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

	@SuppressWarnings("serial")
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
