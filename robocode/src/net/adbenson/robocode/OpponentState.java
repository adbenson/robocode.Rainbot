package net.adbenson.robocode;
import net.adbenson.utility.Vector;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

public class OpponentState extends BotState<OpponentState> {
	
	public final double bearing;
	public final double distance;
	
	public OpponentState(OpponentState previous, OpponentState current) {
		super(
			current.name + "_DIFF",
			current.energy - previous.energy,
			current.heading - previous.heading,
			current.velocity - previous.velocity,
			current.position.subtract(previous.position),
			null
		);
		
		this.bearing = previous.bearing - current.bearing;
		this.distance = previous.distance - current.distance;
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
		double absoluteBearing = absoluteBearing(self.getHeading(), current.getBearingRadians());
		
		Vector relative = Vector.getVectorFromAngle(absoluteBearing, current.getDistance());

		return relative.add(new Vector(self.getX(), self.getY()));
	}

	@Override
	public OpponentState diff(OpponentState previous) {
		return new OpponentState(previous, this);
	}

	public double absoluteBearing(SelfState self) {
		return absoluteBearing(self.heading, bearing);
	}
	
	private static double absoluteBearing(double self, double opponent) {
		return self + opponent;
	}

}