package net.adbenson.robocode;
import net.adbenson.utility.Vector;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;


@SuppressWarnings("serial")
public class BattleState {
	
	OpponentState self;
	OpponentState current;
	OpponentState previous;
	OpponentChange change;
	
	public BattleState(ScannedRobotEvent event, OpponentState previousState) {
		
		this.previous = previousState;
		if (previousState != null) {
			this.change = new OpponentChange(this.current, previousState);
		}
		else {
			this.change = new OpponentChange();
		}
	}
	
	public Vector getPosition(AdvancedRobot other) {
		double bearing = other.getHeadingRadians() + current.getBearingRadians();
		
		Vector relative = Vector.getVectorFromAngle(bearing, current.getDistance());

		return relative.add(other.getX(), other.getY());
	}
	
	/**
	 * Guess if the bot has just stopped
	 * @return true if the bot was moving, and now is not.
	 */
	public boolean stopped() {
		//Check if the bot's current speed is very low, and it wasn't before
		return (Math.abs(current.getVelocity()) < 0.01 && Math.abs(change.getVelocity()) > 0.01);
	}

}
