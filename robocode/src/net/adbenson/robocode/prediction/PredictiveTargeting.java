package net.adbenson.robocode.prediction;

import java.awt.Graphics2D;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import net.adbenson.robocode.botstate.BattleHistory;
import net.adbenson.robocode.botstate.BotState.StateMatchComparator;
import net.adbenson.robocode.botstate.OpponentState;
import net.adbenson.robocode.botstate.OpponentState.PredictiveStateUnavailableException;
import net.adbenson.robocode.bullet.Bullet;
import net.adbenson.utility.Vector;
import robocode.Rules;

public class PredictiveTargeting {
	

	public static final int PREDICTIVE_LOOKBEHIND = 100;
	
	public static final double INITIAL_CONFIDENCE = 0.5;
	
	private final double POWER_RANGE = Rules.MAX_BULLET_POWER - Rules.MIN_BULLET_POWER;
	
	private double confidence;
	private BattleHistory history;
	
	private StateMatchComparator<OpponentState> predictiveComparator;
	
	private LinkedList<OpponentState> opponentPrediction;
	private OpponentState candidateTarget;
	
	public PredictiveTargeting(BattleHistory history) {
		this.history = history;
		this.confidence = INITIAL_CONFIDENCE;
		predictiveComparator = new HeadingVelocityStateComparator();
	}
	
	private double getTargetFirepower() {
		return (POWER_RANGE * confidence) + Rules.MIN_BULLET_POWER;
	}
	
	public Map.Entry<Double, OpponentState> selectTargetFromPrediction(LinkedList<OpponentState> prediction) throws UnableToTargetPredictionException {							
		int turnsToPosition = 0;
		
		TreeMap<Double, OpponentState> potentialTargets = new TreeMap<Double, OpponentState>();
		
		for(OpponentState target : prediction) {
			turnsToPosition++;
						
			double distance = target.position.distance(position);
			double requiredPower = Bullet.getRequiredPower(turnsToPosition, distance);
		
			//If the power required is below the minimum, it can't possibly get there in time.
			if (requiredPower >= Rules.MIN_BULLET_POWER && requiredPower <= Rules.MAX_BULLET_POWER) {
				potentialTargets.put(requiredPower, target);				
			}
		}
		
		//If nothing is added to the list, nothing is in range.
		if (potentialTargets.isEmpty()) {
			throw new UnableToTargetPredictionException();
		}
		
		//Get the closest entry with a required power >= targetPower
		Map.Entry<Double, OpponentState> closestMatch = 
			potentialTargets.ceilingEntry(targetPower);
		
		//If there's none, just take the highest available.
		if (closestMatch == null) {
			closestMatch = potentialTargets.lastEntry();
		}
		
		//Store this so we can draw it later
		candidateTarget = closestMatch.getValue();
		
		return closestMatch;
	}

	public LinkedList<OpponentState> predictTheFuture() throws PredictiveStateUnavailableException, ImpossibleToSeeTheFutureIsException {
		OpponentState o = history.getCurrentOpponent();
		LinkedList<OpponentState> prediction = null;

		long start = System.nanoTime();
		
		OpponentState bestMatch = o.matchStateSequence(PREDICTIVE_LOOKBEHIND, predictiveComparator);
		
		if (bestMatch != null) {
    		prediction = o.predictStates(bestMatch, PREDICTIVE_LOOKBEHIND);
		}
		else {
			throw new ImpossibleToSeeTheFutureIsException();
		}
		
		System.out.print("time:"); System.out.format("%,8d", System.nanoTime() - start);
		System.out.println(" ("+history.getStateCount()+")");

		return prediction;
	}

	public void drawPrediction(Graphics2D g) {
		if (opponentPrediction != null) { 
			for(int i=0; i<opponentPrediction.size(); i++) {
				opponentPrediction.get(i).drawPath(g, i);
			}
		}
	}

	public OpponentState getTarget() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean canPredict(long turn) {
		return turn > PREDICTIVE_LOOKBEHIND;
	}

}
