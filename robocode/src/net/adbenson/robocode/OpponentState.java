package net.adbenson.robocode;
import net.adbenson.utility.Vector;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;


@SuppressWarnings("serial")
public class OpponentState extends Opponent {
	
	Opponent previousState;
	OpponentChange change;
	
	public OpponentState(ScannedRobotEvent event, OpponentState previousState) {
		super(event);
		this.previousState = previousState;
		if (previousState != null) {
			this.change = new OpponentChange(this, previousState);
		}
		else {
			this.change = new OpponentChange();
		}
	}
	
	public Vector getRelativePosition(AdvancedRobot other) {
		double bearing = other.getHeadingRadians() + getBearingRadians();

		double relativeX = Math.sin(bearing) * getDistance();
		double relativeY = Math.cos(bearing) * getDistance();

		return new Vector(relativeX, relativeY);
	}

	public Vector getAbsolutePosition(AdvancedRobot other) {
		Vector relative = getRelativePosition(other);

		return relative.add(new Vector(other.getX(), other.getY()));
	}
	
	/**
	 * Guess if the bot has just stopped
	 * @return true if the bot was moving, and now is not.
	 */
	public boolean stopped() {
		//Check if the bot's current speed is very low, and it wasn't before
		return (Math.abs(getVelocity()) < 0.01 && Math.abs(change.getVelocity()) > 0.01);
	}

}
