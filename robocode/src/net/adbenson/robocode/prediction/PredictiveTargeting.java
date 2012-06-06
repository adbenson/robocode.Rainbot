package net.adbenson.robocode.prediction;

import java.awt.Graphics2D;
import java.util.Collections;
import java.util.LinkedList;

import net.adbenson.robocode.botstate.BattleState;
import net.adbenson.robocode.botstate.OpponentState;
import net.adbenson.robocode.botstate.BotState.StateMatchComparator;
import net.adbenson.robocode.botstate.OpponentState.PredictiveStateUnavailableException;
import net.adbenson.robocode.bullet.Bullet;
import net.adbenson.utility.Vector;
import robocode.Rules;

public class PredictiveTargeting {
	
	//Lookbehind determines how many past states the bot will attempt to match before making a prediction.
	//A high value will generally be more accurate, but more prone to wildly false predictions against random enemies.
	//A higher value will also put a higher load on the processor and increase the time before the first lock.
	public static final int PREDICTION_LOOKBEHIND = 25;
	
	//Lookahead determines how far into the future the bot will predict it's opponents movements.
	//It also determines how far away the bot can target;
	//If it's too short, there won't be enough time for bullet travel.
	//Approx. 50 is enough to target the far corner of the field.
	//Recommend around 38 to target 75% of the field.
	public static final int PREDICTION_LOOKAHEAD = 38;
	
	public static final double PREDICTION_CONFIDENCE_SHIFT = 0.05;
	
	public static final int LOW_CONFIDENCE_TURN_THRESHOLD = 5;

	private static final double LOW_CONFIDENCE_POWER_LIMIT = 0.15;
	
	//Gives a non-linear scale of confidence to firepower, preferring low firepower.
	//(Higher give steeper slope, 1 gives linear scale
	public static final double CONFIDENCE_FIREPOWER_EXP = 2.5;
	
	private final double POWER_RANGE = Rules.MAX_BULLET_POWER - Rules.MIN_BULLET_POWER;
	
	private BattleState history;
	
	private StateMatchComparator<OpponentState> predictiveComparator;
	
	private LinkedList<OpponentState> predictedTargets;
	private LinkedList<PredictedTarget> viableTargets;
	private OpponentState selectedTarget;
	
	private int hits;
	private int misses;
	private double accuracy;
	
	public PredictiveTargeting(BattleState history) {
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
			System.out.println("PERFECT RECORD! Very confident!");
			return 1;
		}
		
		double turns = history.getTurn();
		double historyRatio = turns / PREDICTION_LOOKBEHIND;
		
		//If we have insufficient historical data, we have no confidence
		if (historyRatio < 2) {
			return 0;
		}
		
		double historyFactor = historyRatio * PREDICTION_CONFIDENCE_SHIFT;
		
		//Increase confidence with accuracy and good history
		double confidence = (accuracy / 2) + historyFactor;
		
		System.out.println("Confidence score:"+confidence+" ("+accuracy+" accuracy, "+historyFactor+" history factor)");
		
		return Math.min(1, confidence);		
	}
	
	private double getTargetFirepower(double confidence) {
		return Math.pow((POWER_RANGE * confidence), CONFIDENCE_FIREPOWER_EXP) + Rules.MIN_BULLET_POWER;
	}
	
	public PredictedTarget getNewTarget(Vector position) throws TargetOutOfRangeException, ImpossibleToSeeTheFutureIsException {
		
		predictedTargets = predictTheFuture();
		
		viableTargets = calculateTargets(position, predictedTargets);
		
		PredictedTarget target = selectBestTarget(viableTargets);
		selectedTarget = target.target;
		
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
System.out.println("Target Firepower: "+targetPower);		
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
		OpponentState o = history.getTarget();
		LinkedList<OpponentState> prediction = null;

		long start = System.nanoTime();
		
		OpponentState bestMatch = o.matchStateSequence(PREDICTION_LOOKBEHIND, PREDICTION_LOOKAHEAD, predictiveComparator);
		
		if (bestMatch == null) {
			throw new ImpossibleToSeeTheFutureIsException("No suitably matching history found");
		}
		
		try {
			prediction = o.predictStates(bestMatch, PREDICTION_LOOKBEHIND);
		} catch (PredictiveStateUnavailableException e) {
			throw new ImpossibleToSeeTheFutureIsException("Insufficient future states to project prediction");
		}
		
		System.out.print("time:"); System.out.format("%,8d", System.nanoTime() - start);
		System.out.println(" ("+history.getTurn()+")");

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
		return selectedTarget;
	}
	
	public boolean canPredict(long turn) {
		return turn > PREDICTION_LOOKBEHIND + PREDICTION_LOOKAHEAD;
	}
	
	public void drawPrediction(Graphics2D g) {
		if (viableTargets != null) { 
			for(PredictedTarget target : viableTargets) {
				target.target.drawHighlight(g);
			}
			for(int i=0; i<predictedTargets.size(); i++) {
				predictedTargets.get(i).drawPath(g, i);
			}
		}
	}

	public void missedTarget(robocode.Bullet bullet) {
		//TODO track misses by bot
		misses++;
		updateAccuracy();
	}

	public void hitTarget(String name) {
		//TODO track hits by bot
		hits++;
		updateAccuracy();
	}
	
	private void updateAccuracy() {
		//Avoid divide by zero!
		if (hits == misses) {
			accuracy = 0.5;
		}
		else {
			accuracy = (double)hits / (hits + misses);
		}
	}


}
