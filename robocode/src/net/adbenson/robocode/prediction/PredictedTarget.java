package net.adbenson.robocode.prediction;

import java.util.Comparator;

import net.adbenson.robocode.botstate.OpponentState;

public class PredictedTarget {
	
	public final double requiredPower;
	public final OpponentState target;
	public final int turnsToPosition;
	
	protected static final Comparator<PredictedTarget> powerComparator = new TargetPowerComparator();
	protected static final Comparator<PredictedTarget> turnsComparator = new TurnsToPositionComparator();

	protected PredictedTarget(double requiredPower, OpponentState target, int turnsToPosition) {
		this.requiredPower = requiredPower;
		this.target = target;
		this.turnsToPosition = turnsToPosition;
	}
	
	public static class TargetPowerComparator implements Comparator<PredictedTarget> {

		@Override
		public int compare(PredictedTarget o1, PredictedTarget o2) {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
	
	public static class TurnsToPositionComparator implements Comparator<PredictedTarget> {

		@Override
		public int compare(PredictedTarget o1, PredictedTarget o2) {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}

}
