package net.adbenson.robocode.prediction;

import java.util.Comparator;

import net.adbenson.robocode.botstate.OpponentState;

public class PredictedTarget {
	
	public final double requiredPower;
	
	public final OpponentState target;
	
	protected static final Comparator<PredictedTarget> powerComparator = new TargetPowerComparator();

	protected PredictedTarget(double requiredPower, OpponentState target) {
		this.requiredPower = requiredPower;
		this.target = target;
	}
	
	public static class TargetPowerComparator implements Comparator<PredictedTarget> {

		@Override
		public int compare(PredictedTarget o1, PredictedTarget o2) {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}

}
