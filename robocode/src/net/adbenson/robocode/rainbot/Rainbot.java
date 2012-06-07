package net.adbenson.robocode.rainbot;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;

import net.adbenson.robocode.botstate.BattleState;
import net.adbenson.robocode.botstate.OpponentState;
import net.adbenson.robocode.bullet.BulletQueue;
import net.adbenson.robocode.bullet.OpponentBullet;
import net.adbenson.robocode.prediction.ImpossibleToSeeTheFutureIsException;
import net.adbenson.robocode.prediction.PredictedTarget;
import net.adbenson.robocode.prediction.PredictiveTargeting;
import net.adbenson.robocode.prediction.TargetOutOfRangeException;
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
	
	public static final double MAX_TURN = Math.PI / 5d;
	
	private BattleState history;
	
	private RoundStatus status;
	
	private PredictiveTargeting predictor;
	
	private int preferredDirection;
	
	private static Rectangle2D field;
	private Rectangle2D safety;
	
	private double preferredDistance;
	
	private boolean opponentAlive;
	
	private BotColor color;
	
	public Rainbot() {
		super();
		
		history = new BattleState();
			
		status = new RoundStatus();	
		
		predictor = new PredictiveTargeting(history);
		
		preferredDirection = 1;
		
		color = new BotColor();
	}
	
	public void run() {
		generateBoundries();
		
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
	    	history.addBots(this, status.foundOpponents);
	    	
//			history.addBots(this, e, getTime());
//			
//			if (history.getCurrentOpponent().change != null) {
//				status.opponentEnergyDrop = history.getCurrentOpponent().change.energy <= -Rules.MIN_BULLET_POWER;
//			}
	    	
	    	color.hueShift(this);
	    	
	    	//Square off!
	    	faceOpponent();
	        
	    	detectOpponentFire();
	    	
	    	//Update bullet positions
	    	history.getSelfBullets().updateAll(getTime());
	    	for(BulletQueue<OpponentBullet> queue: history.getAllOpponentBullets()) {
	    		queue.updateAll(getTime());
	    	}	    	
	    	
	    	if (predictor.canPredict(getTime())) {
	    		//TODO decide to predict when the opponent stops moving
	    		
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
						PredictedTarget target = predictor.getNewTarget(getPosition());

						requiredFirepower = target.requiredPower;
						setGunTurnToTarget(target.target);

						ready = true;

					} catch (TargetOutOfRangeException e) {
						System.out.println("Predicted target unreachable");
					} catch (ImpossibleToSeeTheFutureIsException e) {
						System.out.println("Impossible to see, the future is. ("+e.getMessage()+")");
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
	
	private void generateBoundries() {
		Vector botSize = new Vector(getWidth(), getHeight());
	
		field = new Rectangle2D.Double(
				(botSize.x/2)+1, (botSize.y/2)+1, 
				getBattleFieldWidth()-(botSize.x-2), getBattleFieldHeight()-(botSize.y-2)
		);
		safety = new Rectangle2D.Double(
				botSize.x, botSize.y, 
				getBattleFieldWidth()-(botSize.x*2), getBattleFieldHeight()-(botSize.y*2)
		);
	}

	private void setGunTurnToTarget(OpponentState target) {
		Vector offset = target.position.subtract(getPosition());
		double heading = Utility.angleDifference(offset.getAngle(), this.getGunHeadingRadians());
		this.setTurnGunRightRadians(heading);
	}
	
	private void faceOpponent() {
    	if (history.hasTarget()) {
    		OpponentState o = history.getTarget();
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
			OpponentState opponent = history.getTarget();
			Vector opponentPos = opponent.getPosition();

			//Eliminate the possibility of wall crash
			if (!field.contains(opponentPos.toPoint()) && opponent.stopped()) {
				System.out.println("Looks like he crashed!");
			}
			else {
				System.out.println("Opponent fire detected");
//history.opponentFired(getTime());
				preferredDirection = -preferredDirection;
				setAhead(100 * preferredDirection);
			}
		}
		
	}

	private Vector getPosition() {
		return new Vector(getX(), getY());
	}
	
	public void setFire(double power) {	
		robocode.Bullet bullet = super.setFireBullet(power);
		history.selfFired(predictor.getTarget(), bullet, getTime());
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		status.foundOpponents.add(e);
		maintainRadarLock(e);
		history.setTargetName(e.getName());
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
		
		g.setColor(Color.red);
		g.draw(field);
		
		g.setColor(Color.green);
		g.draw(safety);
		g.setStroke(new BasicStroke(3));
		
		for(BulletQueue<OpponentBullet> queue: history.getAllOpponentBullets()) {
			queue.draw(g);
		}
		history.getSelfBullets().draw(g);
		
		for(OpponentState opponent: history.getAllOpponents()) {
			opponent.drawTarget(g);
		}
		history.getSelf().draw(g);
		
		predictor.drawPrediction(g);
	}
		
	public void onBulletHit(BulletHitEvent event)  {
		color.startRainbow();
		status.hitToOpponent = true;
		predictor.hitTarget();
	}
	
	public void onBulletHitBullet(BulletHitBulletEvent event) {
		history.getSelfBullets().remove(event.getBullet());
//		history.getOpponentBullets().remove(event.getHitBullet());
	}
	
	public void onBulletMissed(BulletMissedEvent event) {
		history.getSelfBullets().remove(event.getBullet());
		predictor.missedTarget();
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
		boolean hitByOpponent;
		boolean hitToOpponent;
		boolean collidedWithWall;
		boolean collidedWithOpponent;
		boolean opponentEnergyDrop;
		
		public LinkedList<ScannedRobotEvent> foundOpponents;
		
		public RoundStatus() {
			reset();
		}
		public void reset() {
			hitByOpponent = false;
			hitToOpponent = false;
			collidedWithWall = false;
			collidedWithOpponent = false;
			opponentEnergyDrop = false;
			
			foundOpponents = new LinkedList<ScannedRobotEvent>();
		}
	}	
}
