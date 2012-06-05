package net.adbenson.robocode.prediction;

import java.awt.Graphics2D;
import java.util.Collections;
import java.util.LinkedList;

import net.adbenson.robocode.botstate.BattleHistory;
import net.adbenson.robocode.botstate.BotState.StateMatchComparator;
import net.adbenson.robocode.botstate.OpponentState;
import net.adbenson.robocode.botstate.OpponentState.PredictiveStateUnavailableException;
import net.adbenson.robocode.bullet.Bullet;
import net.adbenson.utility.Vector;
import robocode.Rules;

public class PredictiveTargeting {
	

	public static final int PREDICTION_LOOKBEHIND = 100;
	
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
	
	public PredictedTarget getNewTarget(Vector position) throws TargetOutOfRangeException, ImpossibleToSeeTheFutureIsException {
		
		LinkedList<OpponentState> futureStates = predictTheFuture();
		
		LinkedList<PredictedTarget> potentialTargets = 
				calculateTargets(position, futureStates);
		
		if (potentialTargets.isEmpty()) {
			throw new TargetOutOfRangeException();
		}
		
		PredictedTarget closestMatch = null;
		double targetPower = getTargetFirepower();
		
		//Get the closest entry with a required power >= targetPower
		for (PredictedTarget target : potentialTargets) {
			closestMatch = target;
			
			if (target.requiredPower >= targetPower) {
				break;
			}
		}
		
		return closestMatch;
	}
	
	private LinkedList<OpponentState> predictTheFuture() throws ImpossibleToSeeTheFutureIsException {
		OpponentState o = history.getCurrentOpponent();
		LinkedList<OpponentState> prediction = null;

		long start = System.nanoTime();
		
		OpponentState bestMatch = o.matchStateSequence(PREDICTION_LOOKBEHIND, predictiveComparator);
		
		if (bestMatch == null) {
			throw new ImpossibleToSeeTheFutureIsException("No suitably matching history found");
		}
		
		try {
			prediction = o.predictStates(bestMatch, PREDICTION_LOOKBEHIND);
		} catch (PredictiveStateUnavailableException e) {
			throw new ImpossibleToSeeTheFutureIsException("Insufficient future states to project prediction");
		}
		
		System.out.print("time:"); System.out.format("%,8d", System.nanoTime() - start);
		System.out.println(" ("+history.getStateCount()+")");

		return prediction;
	}
	
	public LinkedList<PredictedTarget> calculateTargets(Vector botPosition, LinkedList<OpponentState> prediction) throws TargetOutOfRangeException {							
		int turnsToPosition = 0;
		
		LinkedList<PredictedTarget> potentialTargets = new LinkedList<PredictedTarget>();
		
		for(OpponentState target : prediction) {
			turnsToPosition++;
						
			double distance = target.position.distance(botPosition);
			double requiredPower = Bullet.getRequiredPower(turnsToPosition, distance);
		
			//If the power required is below the minimum, it can't possibly get there in time.
			if (requiredPower >= Rules.MIN_BULLET_POWER && 
					requiredPower <= Rules.MAX_BULLET_POWER) {
				potentialTargets.add(new PredictedTarget(requiredPower, target));				
			}
		}
		
		//If nothing is added to the list, nothing is in range.
		if (potentialTargets.isEmpty()) {
			throw new TargetOutOfRangeException();
		}
		
		Collections.sort(potentialTargets, PredictedTarget.powerComparator);
		
		return potentialTargets;
	}
	
	public OpponentState getTarget() {
		return candidateTarget;
	}
	
	public boolean canPredict(long turn) {
		return turn > PREDICTION_LOOKBEHIND;
	}
	
	public void drawPrediction(Graphics2D g) {
		if (opponentPrediction != null) { 
			for(int i=0; i<opponentPrediction.size(); i++) {
				opponentPrediction.get(i).drawPath(g, i);
			}
		}
	}

}
