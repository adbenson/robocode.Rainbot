package net.adbenson.robocode.botstate;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.LinkedList;

import net.adbenson.robocode.prediction.PredictiveTargeting;
import net.adbenson.robocode.rainbot.Rainbot;
import net.adbenson.utility.Utility;
import net.adbenson.utility.Vector;
import robocode.Rules;
import robocode.ScannedRobotEvent;

public class OpponentState extends BotState<OpponentState> {
	
	public final double bearing;
	public final double absoluteBearing;
	public final double distance;
	
	private final SelfState self;
	
	private boolean shotBySelf;
	private boolean collidedWithSelf;
	
	private boolean alive;
	
	//Full constructor. Specify all the fields
	public OpponentState(
			String name, double energy, double gunHeat,
			double heading, double velocity, Vector position, 
			SelfState self, OpponentState previous, 
			double bearing, double absoluteBearing, double distance, 
			long turn, boolean alive) {
		super(name, energy, gunHeat, heading, velocity, position, previous, turn);
		this.self = self;
		this.bearing = bearing;
		this.absoluteBearing = absoluteBearing;
		this.distance = distance;
		this.alive = alive;
	}
	
	//Difference constructor. Used for constructing Change states
	public OpponentState(OpponentState a, OpponentState b, boolean add) {
		super(a, b, add);
		
		this.bearing = add? (a.bearing + b.bearing) : 
			Utility.angleDiff(a.bearing, b.bearing);
		
		this.absoluteBearing = add? (a.absoluteBearing + b.absoluteBearing) : 
			Utility.angleDiff(a.absoluteBearing, b.absoluteBearing);
		
		this.distance = a.distance + (add? b.distance : -b.distance);
		
		this.self = null;
	}

	//Initial constructor, used when no previous state is known.
	public OpponentState(ScannedRobotEvent event, SelfState self) {
		this(event, null, self);
	}

	//Subsequent constructor. Used to create all new states after the first
	public OpponentState(ScannedRobotEvent current, OpponentState previous, SelfState self) {		
		super(
				current.getName(),
				current.getEnergy(),
				previous==null? 3 : Math.max(0, previous.gunHeat - Rainbot.getGlobalGunCoolingRate()),
				current.getHeadingRadians(),
				current.getVelocity(),
				calculatePosition(current, self), 
				previous,
				self.turn
		);
		
		this.bearing = current.getBearingRadians();
		this.absoluteBearing = absoluteBearing(self, current);
		this.distance = current.getDistance();
		this.alive = previous==null? true : previous.alive; //What is dead can never die
		
		this.self = null;
	}	

	private static Vector calculatePosition(ScannedRobotEvent current, SelfState self) {
		double absoluteBearing = absoluteBearing(self, current);
		
		Vector relative = Vector.getVectorFromAngleAndLength(absoluteBearing, current.getDistance());

		return relative.add(self.position);
	}
	
	public LinkedList<OpponentState> predictStates(OpponentState basis, int nTurns) throws PredictiveStateUnavailableException{
		LinkedList<OpponentState> nextStates = new LinkedList<OpponentState>();
		//Start with basis.next because basis matches the current state
		OpponentState nextBasis = basis.getNext();
		OpponentState nextState = this;
		
		for(int i = 0; i < nTurns; i++) {
			if (nextBasis == null || nextBasis.change == null) {
				throw new PredictiveStateUnavailableException();
			}
			
			double newHeading = (nextState.heading + nextBasis.change.heading) % Utility.TWO_PI;
			double newVelocity = nextState.velocity + nextBasis.change.velocity;
			
			Vector newPosition = nextState.position.add(Vector.getVectorFromAngleAndLength(newHeading, newVelocity));
						
//			String name, double energy, double heading, double velocity, 
//			Vector position, OpponentState previous, 
//			double bearing, double absoluteBearing, double distance
			nextState = new OpponentState(
					"Prediction", energy, 0, 
					newHeading, newVelocity, newPosition, 
					null, nextState,
					0, 0, 0,
					-1, false
			);
			nextStates.add(nextState);
			
			nextBasis = nextBasis.getNext();	
		}
		
		return nextStates;
	}

	@Override
	public OpponentState diff(OpponentState b) {
		return new OpponentState(this, b, false);
	}
	
	@Override
	public OpponentState sum(OpponentState b) {
		return new OpponentState(this, b, true);
	}
	
	private static double absoluteBearing(SelfState self, ScannedRobotEvent current) {
		return self.heading + current.getBearingRadians();
	}
	
	public void drawPath(Graphics2D g, int index) {
		g.setStroke(new BasicStroke(1));
		
		g.setColor(Utility.setAlpha(Color.green, 0.4));
		position.drawTo(g, heading, velocity * 5);
		
		float ratio = (float)index / PredictiveTargeting.PREDICTION_LOOKBEHIND;
		g.setColor(Color.getHSBColor(ratio, 1f, 1f));		
		g.fillOval(position.intX()-1, position.intY()-1, 2, 2);
	}

	public void drawTarget(Graphics2D g) {
		g.setStroke(new BasicStroke(1));
		g.setColor(Utility.setAlpha(Color.red, 0.9));
		Utility.drawCrosshairs(g, position, 40, 50);
		
		g.setColor(Utility.setAlpha(Color.pink, 0.4));
		
		position.drawTo(g, Utility.oppositeAngle(absoluteBearing), distance / 2);
	}

	public void drawHighlight(Graphics2D g) {
		g.setColor(Utility.setAlpha(Color.white, 0.6));	
		g.fillOval(position.intX()-3, position.intY()-3, 6, 6);
	}
	
	public boolean hasFired() {
		if (change == null) {
			return false;
		}

		boolean energyDropped = change.energy <= -Rules.MIN_BULLET_POWER;
		if (!energyDropped) {
			return false;
		}
		
		System.out.print("Opponent Energy drop: ");
		
		boolean wallCollision = collidedWithWall();
		if (wallCollision) {
			System.out.println("Wall Collision");
		}
		
		if (previous.shotBySelf) {
			System.out.println("Got Shot");
		}
		
		if (previous.collidedWithSelf) {
			System.out.println("Ran into me");
		}
		
		boolean shot = !wallCollision && !previous.shotBySelf && !previous.collidedWithSelf;
		if (shot) {
			System.out.println("Fired bullet @"+(-change.energy));
			gunHeat = 1 + (-change.energy / 5.0);
		}
		
		return shot;
	}

	public void died() {
		alive = false;
	}
	
	public boolean isAlive() {
		return alive;
	}

	public void shotBySelf() {
		shotBySelf = true;
	}
	
	public void collidedWithSelf() {
		collidedWithSelf = true;
	}
	
	public boolean collidedWithWall() {
		boolean offField = !Rainbot.getField().contains(position.toPoint());
		boolean stopped = stopped();
		return  offField && stopped;
	}

	@SuppressWarnings("serial")
	public class PredictiveStateUnavailableException extends Exception {}
}