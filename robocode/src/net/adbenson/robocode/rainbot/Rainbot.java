package net.adbenson.robocode.rainbot;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedList;

import net.adbenson.robocode.botstate.BattleState;
import net.adbenson.robocode.botstate.OpponentState;
import net.adbenson.robocode.bullet.Bullet;
import net.adbenson.robocode.bullet.BulletQueue;
import net.adbenson.robocode.bullet.OpponentBullet;
import net.adbenson.robocode.targeting.FiringController;
import net.adbenson.robocode.targeting.PredictiveTargeting;
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
		
	private static double gunCoolingRate;
	
	private BattleState state;
	
	public LinkedList<ScannedRobotEvent> foundOpponents;
	
	private PredictiveTargeting predictor;
	
	private int preferredDirection;
	private Vector destination;
	
	private static Rectangle2D field;
	private RoundRectangle2D safetyArea;
	private RoundRectangle2D preferredArea;
	
	private BotColor color;
	
	private FiringController firingController;
	
	public Rainbot() {
		super();
		
		state = new BattleState();
		
		predictor = new PredictiveTargeting(state);
		
		preferredDirection = 1;
		
		color = new BotColor();
		
		foundOpponents = new LinkedList<ScannedRobotEvent>();
		
		firingController = new FiringController(getGunCoolingRate());
	}
	
	public void run() {
		generateBoundries();
		gunCoolingRate = this.getGunCoolingRate();
		
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
	    
	    startRadarLock();
	    
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
	    	
	    	updateBulletStates();
	    	
	    	if (firingController.readyToTarget(getGunHeat()) && predictor.canPredict(getTime())) {
	    		OpponentState target = firingController.target(predictor, getPosition());
	    		if (target != null) {
	    			setGunTurnToTarget(target);
	    		}
	    	}
	    	
	    	if (firingController.targetAquired()) {
	    		firingController.checkAim(getGunTurnRemainingRadians());	    		
	    	}
	    	
	    	if (firingController.readyToFire(getGunHeat()) && state.getTarget().isAlive()) {
	    		setFire(firingController.fire());
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
	
	private void setGunTurnToTarget(OpponentState target) {
		Vector offset = target.position.subtract(getPosition());
		double heading = Utility.angleDiff(offset.getAngle(), getGunHeadingRadians());
		setTurnGunRightRadians(heading);
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
//		preferredDirection = -preferredDirection;
		
		destination = getDestination(preferredDirection, move);
			
		//See if our trajectory would take us outside the safety
		if (!preferredArea.contains(destination.toPoint())) {
			destination = getDestination(-preferredDirection, move);
			
			System.out.println("Forward not safe. Reversing");
			if (!preferredArea.contains(destination.toPoint())) {
				System.out.println("Backed into a corner!");
				
				double heading = getHeadingRadians();
				
				do {
					heading += 0.1;
					destination = getDestination(heading, preferredDirection, move);
				} while(!preferredArea.contains(destination.toPoint()));
				
				setTurnHeading(heading);
			}
			else {
				preferredDirection = -preferredDirection;
			}
		}
		
		setAhead(move * preferredDirection);
	}
	
	private Vector getDestination(int direction, double distance) {
		return getDestination(getHeadingRadians(), direction, distance);
	}
	
	private Vector getDestination(double heading, int direction, double distance) {
		if (direction < 1) {
			heading = Utility.oppositeAngle(heading);
		}
		
		return getPosition().project(heading, distance);
	}

	private double distanceFromCenter() {
		Vector center = new Vector(field.getCenterX(), field.getCenterY());
		return getPosition().distance(center);
	}

	public Vector getPosition() {
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
		
		state.getSelfBullets().terminate(event.getBullet(), Bullet.Fate.HIT_TARGET);
	}
	
	//Shot by opponent
	public void onHitByBullet(HitByBulletEvent event) {
		state.getOpponentBullets(event.getBullet().getName()).terminate(event.getBullet(), Bullet.Fate.HIT_TARGET);
		//TODO track hit/miss rate
	}
	
	public void onBulletHitBullet(BulletHitBulletEvent event) {
		state.getSelfBullets().terminate(event.getBullet(), Bullet.Fate.HIT_BULLET);
		robocode.Bullet opponentBullet = event.getHitBullet();
		state.getOpponentBullets(opponentBullet.getName()).terminate(opponentBullet, Bullet.Fate.HIT_BULLET);	
	}
	
	public void onBulletMissed(BulletMissedEvent event) {
		state.getSelfBullets().terminate(event.getBullet(), Bullet.Fate.MISSED);
		predictor.missedTarget(event.getBullet());
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
	
	public static double getGlobalGunCoolingRate() {
		return gunCoolingRate;
	}
}
