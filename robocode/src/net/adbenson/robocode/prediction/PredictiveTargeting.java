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
	
	public static final int PREDICTION_LOOKBEHIND = 20;
	
	public static final double PREDICTION_CONFIDENCE_SHIFT = 0.07;
	
	public static final int LOW_CONFIDENCE_TURN_THRESHOLD = 5;

	private static final double LOW_CONFIDENCE_POWER_LIMIT = 0.15;
	
	private final double POWER_RANGE = Rules.MAX_BULLET_POWER - Rules.MIN_BULLET_POWER;
	
	private BattleHistory history;
	
	private StateMatchComparator<OpponentState> predictiveComparator;
	
	private LinkedList<PredictedTarget> opponentPrediction;
	private OpponentState candidateTarget;
	
	private int hits;
	private int misses;
	private double accuracy;
	
	public PredictiveTargeting(BattleHistory history) {
		this.history = history;
		predictiveComparator = new HeadingVelocityStateComparator();
		
		hits = 0;
		misses = 0;
		updateAccuracy();
	}
	

	private double getConfidence() {
//		//Until we have some hits and misses, we have no confidence.
//		if (hits == 0 && misses == 0) {
//			return 0;
//		}
		//If we've only ever hit, we're very confident
		if (hits > 1 && misses == 0) {
			return 1;
		}
		
		int turns = history.getStateCount();
		double comparableHistories = turns / PREDICTION_LOOKBEHIND;
		
		//If we have insufficient historical data, we have no confidence
		if (comparableHistories < 2) {
			return 0;
		}
		
		//Increase confidence with accuracy and good history
		double confidence = accuracy + (comparableHistories * PREDICTION_CONFIDENCE_SHIFT);
		
		return Math.min(1, confidence);		
	}
	
	private double getTargetFirepower(double confidence) {
		return (POWER_RANGE * confidence) + Rules.MIN_BULLET_POWER;
	}
	
	public PredictedTarget getNewTarget(Vector position) throws TargetOutOfRangeException, ImpossibleToSeeTheFutureIsException {
		
		LinkedList<OpponentState> futureStates = predictTheFuture();
		
		opponentPrediction = calculateTargets(position, futureStates);
		
		if (opponentPrediction.isEmpty()) {
			throw new TargetOutOfRangeException();
		}
		
		PredictedTarget target = selectBestTarget(opponentPrediction);
		candidateTarget = target.target;
		
		return target;
	}
	
	private PredictedTarget selectBestTarget(LinkedList<PredictedTarget> targets) throws ImpossibleToSeeTheFutureIsException{
		double confidence = getConfidence();
		
		if (confidence <= 0) {
			//If confidence is very low, only shoot if the closest target requires little power.
			Collections.sort(targets, PredictedTarget.turnsComparator);
			
			for(PredictedTarget target : targets) {
				if (target.requiredPower <= LOW_CONFIDENCE_POWER_LIMIT &&
						target.turnsToPosition <= LOW_CONFIDENCE_TURN_THRESHOLD) {
					return target;
				}
			}

			throw new ImpossibleToSeeTheFutureIsException("Confidence too low, no easy shots available.");
		}
		
		//Sort by POWER
		Collections.sort(targets, PredictedTarget.powerComparator);
		
		//If confidence is very high, go for the biggest shot
		if (confidence >= 1) {
			return targets.getLast();
		}
		
		//Otherwise, try to find the target power level
		double targetPower = getTargetFirepower(confidence);
		
		//Get the closest entry with a required power >= targetPower
		PredictedTarget closestMatch = null;
		for (PredictedTarget target : targets) {
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
				potentialTargets.add(new PredictedTarget(requiredPower, target, turnsToPosition));				
			}
		}
		
		//If nothing is added to the list, nothing is in range.
		if (potentialTargets.isEmpty()) {
			throw new TargetOutOfRangeException();
		}
		
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
				opponentPrediction.get(i).target.drawPath(g, i);
			}
		}
	}

	public void missedTarget() {
		misses++;
		updateAccuracy();
	}

	public void hitTarget() {
		hits++;
		updateAccuracy();
	}
	
	private void updateAccuracy() {
		if (hits == misses) {
			accuracy = 0.5;
		}
		else {
			accuracy = (double)hits / (hits + misses);
		}
	}


}
