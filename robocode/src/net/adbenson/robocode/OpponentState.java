package net.adbenson.robocode;
import net.adbenson.utility.Vector;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

public class OpponentState extends BotState<OpponentState> {
	
	final double bearing;
	final double distance;
	
	public OpponentState(OpponentState previous, OpponentState current) {
		super(
			current.name + "_DIFF",
			previous.energy - current.energy,
			previous.heading - current.heading,
			previous.velocity - current.velocity,
			previous.position.subtract(current.position),
			null
		);
		
		this.bearing = current.bearing - previous.bearing;
		this.distance = current.distance - previous.distance;
	}

	public OpponentState(ScannedRobotEvent event, AdvancedRobot self) {
		this(event, null, self);
	}

	public OpponentState(ScannedRobotEvent current, OpponentState previous, AdvancedRobot self) {		
		super(
				current.getName(),
				current.getEnergy(),
				current.getHeadingRadians(),
				current.getVelocity(),
				calculatePosition(current, self), 
				previous
		);
		
		this.bearing = current.getBearingRadians();
		this.distance = current.getDistance();
	}	

	private static Vector calculatePosition(ScannedRobotEvent current, AdvancedRobot self) {
		double absoluteBearing = self.getHeading() + current.getBearingRadians();
		
		Vector relative = Vector.getVectorFromAngle(absoluteBearing, current.getDistance());

		return relative.add(new Vector(self.getX(), self.getY()));
	}

	@Override
	public OpponentState diff(OpponentState previous) {
		return new OpponentState(previous, this);
	}

}