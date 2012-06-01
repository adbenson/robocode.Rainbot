package net.adbenson.robocode;

import robocode.AdvancedRobot;

public class SelfState extends BotState<SelfState> {

	public final double gunHeading;

	/**
	 * Differential constructor - only to be used interally for generating change objects.
	 * @param previous
	 * @param current
	 */
	private SelfState(SelfState previous, SelfState current) {
		super(
				current.name+"_DIFF",
				previous.energy - current.energy,
				previous.heading - current.heading,
				previous.velocity - current.velocity,
				previous.position.subtract(current.position),
				null
		);
		
		this.gunHeading = previous.gunHeading - current.gunHeading;
	}
	
	public SelfState(AdvancedRobot self) {
		this(self, null);
	}

	public SelfState(AdvancedRobot self, SelfState previous) {
		super(self, previous);
		this.gunHeading = self.getGunHeading();
	}

	@Override
	public SelfState diff(SelfState previous) {
		return new SelfState(previous, this);
	}

}
