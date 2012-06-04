package net.adbenson.robocode.botstate;

import java.util.LinkedList;

import net.adbenson.utility.Utility;
import net.adbenson.utility.Vector;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

public abstract class BotState<T extends BotState<T>> {
	
	public final String name;
	public final double energy;
	public final double heading;
	public final double velocity;
	public final Vector position;
	
	public final T previous;
	public final T change;
	
	protected BotState(String name, double energy, double heading, double velocity, Vector position, T previous) {
		this.name = name;
		this.energy = energy;
		this.heading = heading;
		this.velocity = velocity;
		this.position = position;
		
		this.previous = previous;
		if (previous == null) {
			change = null;
		}
		else {
			change = this.diff(previous);
		}
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
	
	public T changeOverTurns(int turns) {
		return changeOverTurns(turns, 0);
	}
	
	public T changeOverTurns(int turns, int start) {
		//Case 1: we don't need to travel any deeper, start summing
		if (start <= 0) {
			
			//Case 1a: We're only looking for the change over 1 turn 
			if (turns <= 1) {
				return change;
			}
			//Case 2a: We need to find the sum of this plus t-1 turns
			else {
				return sum(turns-1);
			}
			
		}
		//Case 2: We're not deep enough yet, travel down
		else {
			
			//Case 2a: Don't start summing yet, go at least 1 further
			if (previous != null) {
				return previous.changeOverTurns(turns, start - 1);
			}
			//Case 2b: Can't go any further. We were asked for more than is available. 
			else {
				return null;
			}
			
		}
	}
	
	//Helper method for ChangeOverTurns. It was getting hairy in there
	private T sum(int turns) {
		//Assume we'll return 'change', whether it's null or not
		T changeSum = change;
		
		if (change != null && previous != null) {
			T toAdd = previous.changeOverTurns(turns);
			
			//toAdd will return null if previous doesn't have a change history
			if (toAdd != null) {
				changeSum = change.sum(toAdd);
			}
		}
		
		return changeSum;
	}
	
	private T previous(int n) {
		if (n <= 1 || previous == null) {
			return previous;
		}
		else {
			return previous(n-1); 
		}
	}
	
	public T matchStateSequence(LinkedList<T> sequence) {		
		LinkedList<T>matchSequence = new LinkedList<T>();
		
		T candidateState = this.previous;
		T bestMatch = candidateState;
		double bestMatchDifference = Double.POSITIVE_INFINITY;
		
		testCandidates:
		while(candidateState != null) {
			
			double thisMatchDifference = 0;
			T testState = candidateState;
			
			for(T state : sequence) {
				if (testState == null || testState.change == null){
					//We must be reaching the earliest state data
					break testCandidates;
				}
				
				thisMatchDifference += Math.abs(
						testState.change.heading - state.change.heading);
				
				testState = testState.previous;
			}
			
			if (thisMatchDifference < bestMatchDifference) {
				bestMatch = candidateState;
				bestMatchDifference = thisMatchDifference;
			}
			
			candidateState = candidateState.previous;
		}
		
		return bestMatch;
	}

}
