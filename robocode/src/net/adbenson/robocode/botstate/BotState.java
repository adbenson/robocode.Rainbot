package net.adbenson.robocode.botstate;

import net.adbenson.utility.Utility;
import net.adbenson.utility.Vector;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

public abstract class BotState<T extends BotState<T>> {
	
	public static final double PREDICTIVE_MATCH_SHORTCUT_THRESHOLD = 0.0001;
	
	public final String name;
	public final double energy;
	public final double heading;
	public final double velocity;
	public final Vector position;
	public final int index;
	
	public final T previous;
	private T next;
	public final T change;
	
	@SuppressWarnings("unchecked")
	protected BotState(String name, double energy, double heading, double velocity, Vector position, T previous) {
		this.name = name;
		this.energy = energy;
		this.heading = heading;
		this.velocity = velocity;
		this.position = position;
		
		this.previous = previous;
		if (previous == null) {
			change = null;
			index = 0;
		}
		else {
			change = this.diff(previous);
			((BotState<T>)previous).next = (T) this;
			index = previous.index + 1;
		}
		
		next = null;
	}
	
	protected <U extends BotState<U>>BotState(U a, U b, boolean add) {
		this(
				b.name+"_DIFF",
				
				a.energy + (add? b.energy : -b.energy),
				
				(add? a.heading + b.heading : 
						Utility.angleDifference(a.heading, b.heading)),
						
				a.velocity + (add? b.velocity : -b.velocity),
				
				(add? a.position.add(b.position) 
						: a.position.subtract(b.position)),
			null
		);	
	}

	public BotState(ScannedRobotEvent bot, Vector position) {
		this(
			bot.getName(),
			bot.getEnergy(),
			bot.getHeadingRadians(),
			bot.getVelocity(),
			position,
			null
		);
	}
	
	public BotState(AdvancedRobot bot, T previous) {
		this(
				bot.getName(),
				bot.getEnergy(),
				bot.getHeadingRadians(),
				bot.getVelocity(),
				new Vector(bot.getX(), bot.getY()),
				previous
		);
	}
	
	public Vector getPosition() {
		return position;
	}
	
	public T getNext() {
		return next;
	}
	
	public abstract T diff(T state);
	
	public abstract T sum(T state);
	
	/**
	 * Guess if the bot has just stopped
	 * @return true if the bot was moving, and now is not.
	 */
	public boolean stopped() {
		//Check if the bot's current speed is very low, and it wasn't before
		return (Math.abs(velocity) < 0.01 && Math.abs(change.velocity) > 0.01);
	}
		
	public T previousState(int n) {
		if (n <= 1 || previous == null) {
			return previous;
		}
		else {
			return previous.previousState(n-1); 
		}
	}
	
	public T matchStateSequence(int turnsToMatch, int lookahead, StateMatchComparator<T> compare) {
		//Initialize the first reference state
		@SuppressWarnings("unchecked")
		T reference = (T) this;
		//Find the first match candidate - far enough into history that there's enough to replicate
		T test = previousState(lookahead);
		
		//Initialize the bestMatch and best comparison
		T bestMatch = null;
		double bestMatchDifference = Integer.MAX_VALUE;
		
		//Start looping through test states
		testStates:
		while(test != null) {
			//Compare the test states to the reference states
			double difference;
			try {
				difference = compareStates(reference, test, turnsToMatch, compare);
			} catch (StateComparisonUnavailableException e) {
				//Reached earliest avaialble state
				break testStates;
			}
					
			//See if this match is better
			if (difference < bestMatchDifference) {
				bestMatch = test;
				bestMatchDifference = difference;		
			}
			
			if (difference <= PREDICTIVE_MATCH_SHORTCUT_THRESHOLD) {
				System.out.println("Prediction threshold met. Shortcutting.");
				break testStates;
			}
			
			test = test.previous;
		}
		
		//Return the best match found
		return bestMatch;
	}
	
	public double compareStates(T reference, T test, int nStates, StateMatchComparator<T> compare) throws StateComparisonUnavailableException {
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
	
	public interface StateMatchComparator<U extends BotState<U>> {
		public double compare(U test, U reference) throws StateComparisonUnavailableException;
	}

}
