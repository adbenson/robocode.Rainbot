package net.adbenson.robocode;

import net.adbenson.utility.Vector;
import robocode.AdvancedRobot;

public class SelfState extends BotState<SelfState> {

	private SelfState(SelfState previous, SelfState current) {
		super(
				current.name+"_DIFF",
				previous.energy - current.energy,
				previous.heading - current.heading,
				previous.velocity - current.velocity,
				null
				);
		
		setPosition(previous.getPosition().subtract(current.getPosition()));
	}
	
	public SelfState(AdvancedRobot self) {
		this(self, null);
	}

	public SelfState(AdvancedRobot self, SelfState previous) {
		super(self, previous);
		setPosition(new Vector(self.getX(), self.getY()));
	}

	@Override
	public SelfState diff(SelfState previous) {
		return new SelfState(previous, this);
	}

}
