package net.adbenson.robocode.prediction;

import net.adbenson.robocode.botstate.OpponentState;

public class PredictedTarget {
	
	public final double requiredPower;
	
	public final OpponentState target;

	protected PredictedTarget(double requiredPower, OpponentState target) {
		this.requiredPower = requiredPower;
		this.target = target;
	}

}
