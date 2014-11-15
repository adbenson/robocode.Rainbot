package net.adbenson.robocode.targeting;

import java.text.DecimalFormat;

import net.adbenson.robocode.botstate.OpponentState;
import net.adbenson.utility.Vector;

public class FiringController {
	
	//Min: 0.03 will assure accuracy to the farthest point on the field
	//Max: 0.15 will assure accuracy at 1/4 of field diagonal
	//Lower will increase likelihood of waiting additional turns for gun to turn
	public static final double ACCEPTABLE_GUN_OFFTARGET = 0.07;
	
	private PredictedTarget target;
    
	private boolean targeted;
	private boolean aimed;
	
	private double gunCoolingRate;
	
	public FiringController() {	    
		targeted = false;
		aimed = false;
	}
	
    public boolean readyToTarget(double gunHeat) {
    	return !targeted && gunHeat <= gunCoolingRate;
	}
	
	public OpponentState target(PredictiveTargeting predictor, Vector position) {		
		try {
			target = predictor.getNewTarget(position);	
			targeted = true;
			
			return target.target;
			
		} catch (TargetOutOfRangeException e) {
			System.out.println("Predicted target unreachable");
		} catch (ImpossibleToSeeTheFutureIsException e) {
			System.out.println("Prediction failed: ("+e.getMessage()+")");
		}
		
		return null;
	}
	
	public boolean targetAquired() {
		return targeted && !aimed;
	}
	
	public void checkAim(double turnRemaining) {
		aimed = Math.abs(turnRemaining) < ACCEPTABLE_GUN_OFFTARGET;
	}
	
	public boolean readyToFire(double gunHeat) {
		return aimed && gunHeat <= 0;
	}
	
	public double fire() {
		targeted = false;
		aimed = false;
		
		System.out.println("Shot @"+
				new DecimalFormat("0.00").format(target.requiredPower));
		
		return target.requiredPower;  
	}

	public void setGunCoolingRate(double gunCoolingRate) {
		this.gunCoolingRate = gunCoolingRate;
	}
	
}
