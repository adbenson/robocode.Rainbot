package net.adbenson.robocode.botstate;

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
				a.heading + (add? b.heading : -b.heading),
				a.velocity + (add? b.velocity : -b.velocity),
				add? a.position.add(b.position) 
						: a.position.subtract(b.position),
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

}
