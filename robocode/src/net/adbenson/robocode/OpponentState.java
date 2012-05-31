package net.adbenson.robocode;
import net.adbenson.utility.Vector;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

public class OpponentState extends BotState<OpponentState> {
	
	final double bearing;
	final double distance;

	public OpponentState(ScannedRobotEvent event, AdvancedRobot self) {
		this(event, null, self);
	}

	public OpponentState(ScannedRobotEvent current, OpponentState previous, AdvancedRobot self) {
		this(current.getName(), current.getEnergy(), current.getHeading(), current.getVelocity(), 
				current.getBearingRadians(), current.getDistance(), previous, self);
	}	

	public OpponentState(String name, double energy, double heading, double velocity, 
			double bearing, double distance, OpponentState previous, AdvancedRobot self) {
		super(name, energy, heading, velocity, previous);
		
		this.bearing = bearing;
		this.distance = distance;
		
		if (previous != null) {
			this.setPosition(calculatePosition(self));
		}
	}

	private Vector calculatePosition(AdvancedRobot self) {
		double absoluteBearing = self.getHeading() + this.bearing;
		
		Vector relative = Vector.getVectorFromAngle(absoluteBearing, distance);

		return relative.add(new Vector(self.getX(), self.getY()));
	}


	@Override
	public OpponentState diff(OpponentState previous) {
		OpponentState diff = new OpponentState(
				this.name + "_DIFF",
				previous.energy - this.energy,
				previous.heading - this.heading,
				previous.velocity - this.velocity,
				previous.bearing - this.bearing,
				previous.velocity - this.velocity,
				null,
				null
			);
		
		if (previous != null) {
			diff.setPosition(previous.position.subtract(this.position));
		}
		
		return diff;
	}

}