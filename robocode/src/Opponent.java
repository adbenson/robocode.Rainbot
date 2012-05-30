import java.awt.geom.Point2D;

import robocode.ScannedRobotEvent;

class Opponent extends ScannedRobotEvent {

	public Opponent(ScannedRobotEvent event) {
		// ScannedRobotEvent(String name, double energy, double bearing, double
		// distance, double heading, double velocity)
		super(event.getName(), event.getEnergy(), event.getBearingRadians(),
				event.getDistance(), event.getHeading(), event.getVelocity());
	}

	public Opponent diff(Opponent previousState) {
		// ScannedRobotEvent(String name, double energy, double bearing, double
		// distance, double heading, double velocity)
		ScannedRobotEvent diff = new ScannedRobotEvent(
				previousState.getName() + " change", 
				this.getEnergy() - previousState.getEnergy(),
				this.getBearing() - previousState.getBearingRadians(),
				this.getDistance() - previousState.getDistance(),
				this.getHeading() - previousState.getEnergy(),
				this.getVelocity() - previousState.getVelocity()
		);
		return new Opponent(diff);
	}

	public Point2D.Double getRelativePosition(Rainbot other) {
		double bearing = other.getHeadingRadians() + getBearingRadians();

		double relativeX = Math.sin(bearing) * getDistance();
		double relativeY = Math.cos(bearing) * getDistance();

		return new Point2D.Double(relativeX, relativeY);
	}

	public Point2D.Double getAbsolutePosition(Rainbot other) {
		Point2D relative = getRelativePosition(other);

		return new Point2D.Double(
				other.getThisBot().getX() + relative.getX(),
				other.getThisBot().getY() + relative.getY()
		);
	}

}