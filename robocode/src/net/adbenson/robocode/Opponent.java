package net.adbenson.robocode;
import robocode.ScannedRobotEvent;

@SuppressWarnings("serial")
public abstract class Opponent extends ScannedRobotEvent {

	public Opponent(ScannedRobotEvent event) {
		// ScannedRobotEvent(String name, double energy, double bearing, double distance, double heading, double velocity)
		super(event.getName(), event.getEnergy(), event.getBearingRadians(),
				event.getDistance(), event.getHeading(), event.getVelocity());
	}

	public Opponent(String string, double d, double e, double f, double g, double h) {
		// ScannedRobotEvent(String name, double energy, double bearing, double distance, double heading, double velocity)
		super(string, d, e, f, g, h);
	}

}