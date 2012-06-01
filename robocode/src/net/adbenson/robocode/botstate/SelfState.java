package net.adbenson.robocode.botstate;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import net.adbenson.utility.Vector;


import robocode.AdvancedRobot;

public class SelfState extends BotState<SelfState> {

	public final double gunHeading;
	public final double gunHeat;

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
		this.gunHeat = previous.gunHeat - current.gunHeat;
	}
	
	public SelfState(AdvancedRobot self) {
		this(self, null);
	}

	public SelfState(AdvancedRobot self, SelfState previous) {
		super(self, previous);
		this.gunHeading = self.getGunHeading();
		this.gunHeat = self.getGunHeat();
	}

	@Override
	public SelfState diff(SelfState previous) {
		return new SelfState(previous, this);
	}

	public void draw(Graphics2D g) {
		g.setColor(velocity > 0? Color.green : Color.red);
		g.setStroke(new BasicStroke(5));

		position.drawTo(g, heading, velocity * 5);
		
//		g.drawLine(position.intX() - 30, position.intY() - 30, position.intX() - 30, )
	}

}