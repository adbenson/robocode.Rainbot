package net.adbenson.robocode.botstate;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.text.DecimalFormat;

import net.adbenson.robocode.bullet.Bullet;
import net.adbenson.robocode.targeting.PredictiveTargeting;
import net.adbenson.utility.Utility;
import net.adbenson.utility.Vector;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

public abstract class BotState
	<BulletType extends Bullet, 
	BotType extends BotState<BulletType, BotType>> {
	
	public static final double INITIAL_GUN_HEAT = 3.0;
	
	public final String name;
	public final double energy;
	public final double heading;
	public final double velocity;
	public final Vector position;
	public final double turn;
	public double gunHeat;
	
	public final BotType previous;
	private BotType next;
	public final BotType change;
	
	private BulletType shotBullet;
	
	@SuppressWarnings("unchecked")
	protected BotState(
			String name, 
			double energy, double gunHeat, double heading, 
			double velocity, Vector position, 
			BotType previous, double turn) {
		this.name = name;
		this.energy = energy;
		this.gunHeat = gunHeat;
		this.heading = heading;
		this.velocity = velocity;
		this.position = position;
		this.turn = turn;
		
		this.previous = previous;
		if (previous == null) {
			change = null;
		}
		else {
			change = this.diff(previous);
			((BotState<BulletType, BotType>)previous).next = (BotType) this;
		}
		
		next = null;
	}
	
	protected <_BotState extends BotState<?, _BotState>>BotState(_BotState a, _BotState b, boolean add) {
		this(
				b.name+"_DIFF",
				
				a.energy + (add? b.energy : -b.energy),
				
				a.gunHeat + (add? b.gunHeat : -b.gunHeat),
				
				(add? a.heading + b.heading : 
						Utility.angleDiff(a.heading, b.heading)),
						
				a.velocity + (add? b.velocity : -b.velocity),
				
				(add? a.position.add(b.position) 
						: a.position.subtract(b.position)),
			null,
			(a.turn + b.turn) / 2.0
		);	
	}

	public BotState(ScannedRobotEvent bot, Vector position, long turn) {
		this(
			bot.getName(),
			bot.getEnergy(),
			INITIAL_GUN_HEAT,
			bot.getHeadingRadians(),
			bot.getVelocity(),
			position,
			null,
			turn
		);
	}
	
	public BotState(AdvancedRobot bot, BotType previous) {
		this(
				bot.getName(),
				bot.getEnergy(),
				bot.getGunHeat(),
				bot.getHeadingRadians(),
				bot.getVelocity(),
				new Vector(bot.getX(), bot.getY()),
				previous,
				bot.getTime()
		);
	}
	
	public Vector getPosition() {
		return position;
	}
	
	public BotType getNext() {
		return next;
	}
	
	public abstract BotType diff(BotType state);
	
	public abstract BotType sum(BotType state);
	
	/**
	 * Guess if the bot has just stopped
	 * @return true if the bot was moving, and now is not.
	 */
	public boolean stopped() {
		//Check if the bot's current speed is very low, and it wasn't before
		return (Math.abs(velocity) < 0.01 && Math.abs(change.velocity) > 0.01);
	}
		
	public BotType previousState(int n) {
		if (n <= 1 || previous == null) {
			return previous;
		}
		else {
			return previous.previousState(n-1); 
		}
	}
	
	public BotType matchStateSequence(int turnsToMatch, int lookahead, StateMatchComparator<BotType> compare) {
		//Initialize the first reference state
		@SuppressWarnings("unchecked")
		BotType reference = (BotType) this;
		//Find the first match candidate - far enough into history that there's enough to replicate
		BotType test = previousState(lookahead);
		
		//Initialize the bestMatch and best comparison
		BotType bestMatch = null;
		double bestMatchDifference = Double.POSITIVE_INFINITY;
		
		//Start looping through test states
		testStates:
		while(test != null) {
			//Compare the test states to the reference states
			double difference;
			try {
				difference = compareStates(reference, test, turnsToMatch, compare);
			} catch (StateComparisonUnavailableException e) {
				if (bestMatchDifference == Double.POSITIVE_INFINITY) {
					System.out.println("Ran out of comparison states before one was matched.");
				}
				break testStates;
			}
					
			//See if this match is better
			if (difference < bestMatchDifference) {
				bestMatch = test;
				bestMatchDifference = difference;		
			}
			
			if (difference <= PredictiveTargeting.PREDICTIVE_MATCH_SHORTCUT_THRESHOLD) {
				System.out.println("Prediction threshold met:"+new DecimalFormat("0.000000000000000").format(difference));
				break testStates;
			}
			
			test = test.previous;
		}
		
		//Return the best match found
		return bestMatch;
	}
	
	public double compareStates(BotType reference, BotType test, int nStates, StateMatchComparator<BotType> compare) 
			throws StateComparisonUnavailableException {
		double difference = 0;
		
		for(int i = 0; i < nStates; i++) {
			if (reference == null || test == null) {
				throw new StateComparisonUnavailableException();
			}
			
			difference += Math.abs(compare.compare(reference, test));
			
			reference = reference.previous;
			test = test.previous;
		}

		return difference;
	}
	
	@SuppressWarnings("serial")
	public static class StateComparisonUnavailableException extends Exception {
		
	}
	
	public interface StateMatchComparator<_BotState extends BotState<?, _BotState>> {
		public double compare(_BotState test, _BotState reference) throws StateComparisonUnavailableException;
	}
	
	public void drawGunHeat(Graphics2D g) {
		g.setStroke(new BasicStroke(4));
		
		g.setColor(Color.blue);
		g.drawLine(
				position.intX() - 30, position.intY() - 30, 
				position.intX() - 30, position.intY()
		);		
		
		if (gunHeat > 0) {
		g.setColor(Color.red);
			g.drawLine(
					position.intX() - 30, position.intY() - 30, 
					position.intX() - 30, (position.intY() - 30) + (int)((gunHeat / 1.6) * 30)
			);
		}
	}

	protected void setShotBullet(BulletType shotBullet) {
		this.shotBullet = shotBullet;
	}

	public BulletType getShotBullet() {
		return shotBullet;
	}

}
