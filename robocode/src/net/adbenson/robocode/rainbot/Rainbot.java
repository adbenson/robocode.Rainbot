package net.adbenson.robocode.rainbot;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
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
	
	public static final double MAX_TURN = Math.PI / 5d;
	
	//Min: 0.03 will assure accuracy to the farthest point on the field
	//Max: 0.15 will assure accuracy at 1/4 of field diagonal
	//Lower will increase likelihood of waiting additional turns for gun to turn
	public static final double ACCEPTABLE_GUN_OFFTARGET = 0.07;
	
	private BattleState state;
	
	public LinkedList<ScannedRobotEvent> foundOpponents;
	
	private PredictiveTargeting predictor;
	
	private int preferredDirection;
	
	private static Rectangle2D field;
	private RoundRectangle2D safety;
	private RoundRectangle2D preferredRadius;
	
	private double preferredDistance;
	
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
		
		preferredDistance = Rules.RADAR_SCAN_RADIUS / 3.0;		
		
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
		
		safety = new RoundRectangle2D.Double(
				botSize.x, botSize.y, 
				getBattleFieldWidth()-(botSize.x*2), getBattleFieldHeight()-(botSize.y*2),
				175, 175
		);
		
		preferredRadius = new RoundRectangle2D.Double(
				botSize.x*1.5, botSize.y*1.5, 
				getBattleFieldWidth()-(botSize.x*3), getBattleFieldHeight()-(botSize.y*3),
				250, 250
		);
	}

	private void setGunTurnToTarget(OpponentState target) {
		Vector offset = target.position.subtract(getPosition());
		double heading = Utility.angleDifference(offset.getAngle(), this.getGunHeadingRadians());
		this.setTurnGunRightRadians(heading);
	}
	
	private void setTurnHeading(double heading) {
		setTurnRight(Utility.angleDifference(heading, getHeadingRadians()));
	}
	
	private void faceOpponent() {
    	if (state.hasTarget()) {
    		OpponentState o = state.getTarget();
    		
    		double offFace = o.bearing;
    		//We don't care which direction we face, so treat either direction the same.
    		if (offFace < 0) {
    			offFace += Math.PI;
    		}
    		   		
    		//Offset so that "facing" is 0
    		offFace -= Utility.HALF_PI;
    		
    		//Turn farther away the closer we are - by preferredDistance away, straighten out
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
//		double oppDistance = bullet.bot.position.distance(getPosition());
		
		int direction = 1;
		
		Vector destination = destination(direction, move);
			
		//See if our trajectory would take us outside the safety
		if (!safety.contains(destination.toPoint())) {
			destination = destination(direction, move);
			
			if (!safety.contains(destination.toPoint())) {
				//Hm...
				System.out.println("No safe path found");
			}
			else {
				direction = -1;
			}
		}
	
		setAhead(move * direction);
	}
	
	private Vector destination(int direction, double distance) {
		double heading = getHeading();
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
		g.draw(safety);
		
		g.setColor(Color.green);
		g.draw(preferredRadius);
		
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
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		foundOpponents.add(e);
		maintainRadarLock(e);
		state.setTargetName(e.getName());
	}
	
	public void onRobotDeath(RobotDeathEvent event) {
		state.getOpponent(event.getName()).died();
	}
	
	//Shot opponent with bullet
	public void onBulletHit(BulletHitEvent event)  {
		color.startRainbow();
		predictor.hitTarget(event.getName());
		state.getOpponent(event.getName()).shotBySelf();
	}
	
	public void onBulletHitBullet(BulletHitBulletEvent event) {
		state.getSelfBullets().remove(event.getBullet());
		//TODO remove opponent bullets		
	}
	
	public void onBulletMissed(BulletMissedEvent event) {
		state.getSelfBullets().remove(event.getBullet());
		predictor.missedTarget(event.getBullet());
	}
	
	//Shot by opponent
	public void onHitByBullet(HitByBulletEvent event) {
		//TODO avoid?
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
