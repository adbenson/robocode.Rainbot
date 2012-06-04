package net.adbenson.robocode.rainbot;

import robocode.Rules;
import net.adbenson.robocode.botstate.BotState;
import net.adbenson.robocode.botstate.OpponentState;
import net.adbenson.robocode.botstate.BotState.StateComparisonUnavailableException;
import net.adbenson.robocode.botstate.BotState.StateMatchComparator;

public class HeadingVelocityStateComparator implements StateMatchComparator<OpponentState> {

	public double compare(OpponentState o1, OpponentState o2) throws StateComparisonUnavailableException {
		if (o1.change == null || o2.change == null) {
			throw new StateComparisonUnavailableException();
		}
		else {
			
//			System.out.println("H: a)"+o1.change.heading+", b)"+o2.change.heading+"\tV: a)"+o1.change.velocity+" b)"+o2.change.velocity);
			
			double headingDiff = Math.abs(o1.change.heading - o2.change.heading);
			double velocityDiff = Math.abs(o1.change.velocity - o2.change.velocity);
			
			//Adjust values for their range, to avoid weighting one or the other
			headingDiff /= Math.PI; 
			velocityDiff /= Rules.MAX_VELOCITY;
//			System.out.println(headingDiff + velocityDiff);						
			return headingDiff + velocityDiff;
		}
	}
	
}
