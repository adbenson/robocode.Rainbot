package net.adbenson.robocode.bullet;
import java.awt.Graphics2D;

import net.adbenson.robocode.botstate.BotState;
import net.adbenson.utility.Vector;


public abstract class Bullet {
	
	public final BotState<?> bot;
	
	public final Vector origin;
	public final double power;
	public final double heading;
	public final long time;
	public final double velocity;
	
	private double distanceTravelled;
	
	public Bullet(BotState<?> bot, double heading, long time) {
		this.bot = bot;
		this.origin = bot.position;
		this.power = -bot.change.energy;
		this.heading = heading;
		this.time = time;
		this.velocity = velocity();
	}
	
	public void updateDistance(long currentTime) {
		long timeElapsed = currentTime - time;
		//A lot of trial and error to get this fudge factor right!
		distanceTravelled = velocity * (timeElapsed + 2.1);
		
		updateProjection();
	}

	private double velocity() {
        return getVelocity(power);
    }
	
	public static double getVelocity(double power) {
		return (20.0 - (3.0 * power));
	}
    
    public double getDistanceTravelled() {
    	return distanceTravelled;
    }

	public abstract void draw(Graphics2D g);
	
	public abstract boolean shouldDelete();
	
    public abstract void updateProjection();
	
}
