package net.adbenson.robocode;

import net.adbenson.utility.Vector;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

public abstract class BotState<T extends BotState<T>> {
	
	public final String name;
	public final double energy;
	public final double heading;
	public final double velocity;
	
	public Vector position;
	
	final T previous;
	final T change;
	
	protected BotState(String name, double energy, double heading, double velocity, T previous) {
		this.name = name;
		this.energy = energy;
		this.heading = heading;
		this.velocity = velocity;
		
		this.previous = previous;
		if (previous == null) {
			change = null;
		}
		else {
			change = this.diff(previous);
		}
	}

	public BotState(ScannedRobotEvent bot) {
		this(
			bot.getName(),
			bot.getEnergy(),
			bot.getHeading(),
			bot.getVelocity(),
			null
		);
	}
	
	public BotState(AdvancedRobot bot, T previous) {
		this(
				bot.getName(),
				bot.getEnergy(),
				bot.getHeading(),
				bot.getVelocity(),
				previous
		);
	}
	
	public Vector getPosition() {
		return position;
	}
	
	protected void setPosition(Vector position) {
		this.position = position;
	}
	
	public abstract T diff(T previous);
	
	/**
	 * Guess if the bot has just stopped
	 * @return true if the bot was moving, and now is not.
	 */
	public boolean stopped() {
		//Check if the bot's current speed is very low, and it wasn't before
		return (Math.abs(velocity) < 0.01 && Math.abs(change.velocity) > 0.01);
	}

}
