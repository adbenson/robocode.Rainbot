package net.adbenson.robocode.rainbot;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.text.DecimalFormat;
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
	
	public static final double TURN_FACTOR = 5.0;
	
	public static final double MAX_TURN_OFF_FACE = Math.PI / TURN_FACTOR;
	
	public static final double PREFERRED_DISTANCE = Rules.RADAR_SCAN_RADIUS / (TURN_FACTOR / 2.0);
	
	//Min: 0.03 will assure accuracy to the farthest point on the field
	//Max: 0.15 will assure accuracy at 1/4 of field diagonal
	//Lower will increase likelihood of waiting additional turns for gun to turn
	public static final double ACCEPTABLE_GUN_OFFTARGET = 0.07;
	
	private BattleState state;
	
	public LinkedList<ScannedRobotEvent> foundOpponents;
	
	private PredictiveTargeting predictor;
	
	private int preferredDirection;
	private Vector destination;
	
	private static Rectangle2D field;
	private RoundRectangle2D safetyArea;
	private RoundRectangle2D preferredArea;
	
	private BotColor color;
	
	public Rainbot() {
		super();
		
		state = new BattleState();
		
		predictor = new PredictiveTargeting(state);
		
		preferredDirection = 1;
		
		color = new BotColor();
		
		foundOpponents = new LinkedList<ScannedRobotEvent>();
	}
	
	public void run() {
		generateBoundries();
		
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
	    
	    startRadarLock();
	    
	    double requiredFirepower = 0;
		boolean ready = false;
		boolean aim = false;
		boolean fire = false;
		
		long turnTargeted = 0;
	    
	    do {
	    	//Store the current state of this bot and any others scanned
	    	state.addBots(this, foundOpponents);
	    	
	    	if (foundOpponents.isEmpty()) {
	    		//Uh-oh... we've lost track of our opponent
	    		System.out.print("Opponent Lost: ");
	    		startRadarLock();
	    	}
	    	
	    	//Reset the list of scanned robots
	    	foundOpponents = new LinkedList<ScannedRobotEvent>();

	    	color.hueShift(this);
	    	
	    	//Square off!
	    	faceOpponent();
	    	
	    	OpponentBullet bullet = detectOpponentFire();
	    	if (bullet != null) {
	    		dodge(bullet);
	    	}
	    	
//	    	avoidWalls();
	    	
	    	updateBulletStates();
	    	
	    	if (predictor.canPredict(getTime())) {
	    		//TODO decide to predict when the opponent stops moving

		    	//ONLY look into prediction if we're not preparing to fire or have recently fired 
	    		if (!ready && this.getGunHeat() <= getGunCoolingRate()) {
	    			
					try {
						PredictedTarget target = predictor.getNewTarget(getPosition());

						requiredFirepower = target.requiredPower;
						setGunTurnToTarget(target.target);

						ready = true;
						turnTargeted = getTime();
						
					} catch (TargetOutOfRangeException e) {
						System.out.println("Predicted target unreachable");
					} catch (ImpossibleToSeeTheFutureIsException e) {
						System.out.println("Prediction failed: ("+e.getMessage()+")");
					}
		    		
	    			aim = false;
	    			fire = false;
	    		}   	
	    		
		    	if (ready && 
		    			Math.abs(this.getGunTurnRemainingRadians()) < ACCEPTABLE_GUN_OFFTARGET &&
		    			this.getGunHeat() <= 0) {	    		
		    		aim = true;
		    		fire = false;
		    	}
	    		
		    	if (state.getTarget().isAlive() && ready && aim && !fire) {
		    		setFire(requiredFirepower);   		
		    		ready = false;
		    		aim = false;
		    		fire = true;
		    		
		    		System.out.println("Shot @"+
		    				new DecimalFormat("0.00").format(requiredFirepower)+" with delay of "+
		    				(getTime()-turnTargeted)+" turns");
		    	}

	    	
	    	}

	    	execute();

	    } while (true);
	}
	
	private void updateBulletStates() {
    	//Update bullet positions
    	state.getSelfBullets().updateAll(getTime());
    	for(BulletQueue<OpponentBullet> queue: state.getAllOpponentBullets()) {
    		queue.updateAll(getTime());
    	}	  
	}

	private void generateBoundries() {
		Vector botSize = new Vector(getWidth(), getHeight());
	
		field = new Rectangle2D.Double(
				(botSize.x/2), (botSize.y/2), 
				getBattleFieldWidth()-(botSize.x), getBattleFieldHeight()-(botSize.y)
		);
		
		safetyArea = new RoundRectangle2D.Double(
				botSize.x, botSize.y, 
				getBattleFieldWidth()-(botSize.x*2), getBattleFieldHeight()-(botSize.y*2),
				175, 175
		);
		
		preferredArea = new RoundRectangle2D.Double(
				botSize.x*1.5, botSize.y*1.5, 
				getBattleFieldWidth()-(botSize.x*3), getBattleFieldHeight()-(botSize.y*3),
				250, 250
		);
	}

	private void setGunTurnToTarget(OpponentState target) {
		Vector offset = target.position.subtract(getPosition());
		double heading = Utility.angleDiff(offset.getAngle(), this.getGunHeadingRadians());
		this.setTurnGunRightRadians(heading);
	}
	
	private void setTurnHeading(double heading) {
		setTurnRight(Utility.angleDiff(heading, getHeadingRadians()));
	}
	
	private void faceOpponent() {
    	if (state.hasTarget()) {
    		OpponentState o = state.getTarget();
    		
    		//Turn farther away the closer we are - by preferredDistance away, straighten out
    		//negative means turn away, positive means turn towards
    		double distanceRatio = (o.distance - PREFERRED_DISTANCE) / (PREFERRED_DISTANCE);
    		//How far we want to turn off-face
    		double offFace = (distanceRatio * MAX_TURN_OFF_FACE) * preferredDirection; 		
    		
    		double turn;
    		//Opponent is on our left
    		if (o.bearing < 0) {
    			double diffFromNormal = Utility.angleSum(o.bearing, Utility.HALF_PI);
    			
    			turn = diffFromNormal - offFace;
    		}
    		//Opponent is on our right
    		else {
    			double diffFromNormal = Utility.angleDiff(o.bearing, Utility.HALF_PI);
    			
    			turn = diffFromNormal + offFace;
    		}
    		
    		//Multiply the offset - we don't have all day! Move it! (If it's too high, it introduces jitter.)
    		setTurnRight(turn * 10);     		
    	}
    	else {
    		//Nuthin' better to do...
    		setTurnRight(Double.POSITIVE_INFINITY);
    	}
	}
	
	private OpponentBullet detectOpponentFire() {
		OpponentBullet focus = null;
		OpponentBullet bullet = null;
		
		//Check all opponents for firing
		for (OpponentState opponent : state.getAllOpponents()) {
			if (opponent.hasFired()) {
				//Get the bullet details
				bullet = state.opponentFired(opponent.name, getTime());
				//If this was fired by our main opponent, focus on it.
				if (opponent.name.equals(state.getTargetName())) {
					focus = bullet;
				}
			}
		}
		
		//If there's no bullet from our main opponent, send any bullet we might have found.
		if (focus == null && bullet != null) {
			return bullet;
		}
		
		return focus;
	}
	
	public static int accelerationTurns(int direction, double current) {
		double changeInVelocity = ((direction * Rules.MAX_VELOCITY) - current);
		return (int)Math.ceil(changeInVelocity / Rules.ACCELERATION);		
	}
	
	public static int stoppingTurns() {
		return (int)Math.ceil(Rules.MAX_VELOCITY / Rules.DECELERATION);
	}
	
	private void dodge(OpponentBullet bullet) {
		double move = bullet.getEscapeDistance() + stoppingTurns();
		preferredDirection = -preferredDirection;
		
		destination = destination(preferredDirection, move);
			
		//See if our trajectory would take us outside the safety
		if (!preferredArea.contains(destination.toPoint())) {
			destination = destination(-preferredDirection, move);
			
			System.out.println("Forward not safe. Reversing");
			if (!preferredArea.contains(destination.toPoint())) {
				System.out.println("No safe path found");
			}
			
			preferredDirection = -preferredDirection;
		}
		
		setAhead(move * preferredDirection);
	}
	
	private Vector destination(int direction, double distance) {
		double heading = getHeadingRadians();
		if (direction < 1) {
			heading = Utility.oppositeAngle(heading);
		}
		
		return getPosition().project(heading, distance);
	}

	private double distanceFromCenter() {
		Vector center = new Vector(field.getCenterX(), field.getCenterY());
		return getPosition().distance(center);
	}

	private Vector getPosition() {
		return new Vector(getX(), getY());
	}
	
	public void setFire(double power) {	
		robocode.Bullet bullet = super.setFireBullet(power);
		state.selfFired(predictor.getTarget(), bullet, getTime());
	}
	
	private void startRadarLock() {
		System.out.println("Searching for Opponent");
	    setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
	}
	
	private void maintainRadarLock(ScannedRobotEvent e) {
		double radarTurn = getHeadingRadians() + e.getBearingRadians() - getRadarHeadingRadians();
		setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn) * 1.9);
	}
	
	public void onPaint(Graphics2D g) {
		
		g.setColor(Color.red);
		g.draw(field);
		
		g.setColor(Color.yellow);
		g.draw(safetyArea);
		
		g.setColor(Color.green);
		g.draw(preferredArea);
		
		g.setStroke(new BasicStroke(3));
		
		for(BulletQueue<OpponentBullet> queue: state.getAllOpponentBullets()) {
			queue.draw(g);
		}
		state.getSelfBullets().draw(g);
		
		for(OpponentState opponent: state.getAllOpponents()) {
			opponent.drawTarget(g);
			opponent.drawGunHeat(g);
		}
		state.getSelf().draw(g);
		
		predictor.drawPrediction(g);
		
		if (destination != null) {
			g.setColor(preferredDirection > 0? Color.green : Color.red);
			g.setStroke(new BasicStroke(3));
			Utility.drawCrosshairs(g, destination, 2, 10);
		}
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		foundOpponents.add(e);
		maintainRadarLock(e);
		state.setTargetName(e.getName());
	}
	
	public void onRobotDeath(RobotDeathEvent event) {
		state.getOpponent(event.getName()).died();
		color.startRainbow();
	}
	
	//Shot opponent with bullet
	public void onBulletHit(BulletHitEvent event)  {
		color.startRainbow();
		predictor.hitTarget(event.getName());
		state.getOpponent(event.getName()).shotBySelf();
		
		state.getSelfBullets().remove(event.getBullet());
	}
	
	public void onBulletHitBullet(BulletHitBulletEvent event) {
		state.getSelfBullets().remove(event.getBullet());
		state.getOpponentBullets(event.getBullet().getName()).remove(event.getBullet());	
	}
	
	public void onBulletMissed(BulletMissedEvent event) {
//		state.getSelfBullets().remove(event.getBullet());
		predictor.missedTarget(event.getBullet());
	}
	
	//Shot by opponent
	public void onHitByBullet(HitByBulletEvent event) {
		state.getOpponentBullets(event.getBullet().getName()).remove(event.getBullet());
		//TODO track hit/miss rate
	}
	
	//Collision with self
	public void onHitRobot(HitRobotEvent event) {
		state.getOpponent(event.getName()).collidedWithSelf();
	}
	
	public void onHitWall(HitWallEvent event) {
		//TODO avoid?
	}
	
	public static Rectangle2D getField() {
		return field;
	}
}
