package net.adbenson.robocode.targeting;

import java.text.DecimalFormat;

import net.adbenson.robocode.botstate.OpponentState;
import net.adbenson.robocode.rainbot.Rainbot;
import net.adbenson.utility.Utility;
import net.adbenson.utility.Vector;

public class FiringController {
	
	//Min: 0.03 will assure accuracy to the farthest point on the field
	//Max: 0.15 will assure accuracy at 1/4 of field diagonal
	//Lower will increase likelihood of waiting additional turns for gun to turn
	public static final double ACCEPTABLE_GUN_OFFTARGET = 0.07;
	
	private PredictedTarget target;
    
	private boolean ready;
	private boolean aim;
	private boolean fire;
	
	private Rainbot self;
	
	public FiringController() {	    
		ready = true;
		aim = false;
		fire = false;
	}
	
	public double fire() {
 			
		ready = false;
		aim = false;
		fire = true;
		
		System.out.println("Shot @"+
				new DecimalFormat("0.00").format(target.requiredPower));
		
		return target.requiredPower;  
	}
	
	public void aim() {
		setGunTurnToTarget(target.target);
		
    	if (ready && 
    			Math.abs(self.getGunTurnRemainingRadians()) < ACCEPTABLE_GUN_OFFTARGET &&
    			self.getGunHeat() <= 0) {	    		
    		aim = true;
    		fire = false;
    	}
	}
	
	private void setGunTurnToTarget(OpponentState target) {
		Vector offset = target.position.subtract(self.getPosition());
		double heading = Utility.angleDiff(offset.getAngle(), self.getGunHeadingRadians());
		self.setTurnGunRightRadians(heading);
	}
	
    public boolean readyToTarget() {
    	return !ready && self.getGunHeat() <= self.getGunCoolingRate();
	}

	public void predictTarget(PredictiveTargeting predictor, long turn) {
		try {
			target = predictor.getNewTarget(self.getPosition());	

			ready = true;
			
		} catch (TargetOutOfRangeException e) {
			System.out.println("Predicted target unreachable");
		} catch (ImpossibleToSeeTheFutureIsException e) {
			System.out.println("Prediction failed: ("+e.getMessage()+")");
		}
		
		aim = false;
		fire = false;
	}

	public boolean readyToFire() {
		return ready && aim && !fire;
	}

	public boolean targetAquired() {
		return target != null && ready;
	}

}
