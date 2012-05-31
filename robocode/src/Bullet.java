import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;


public abstract class Bullet {
	
	final Point2D origin;
	final double power;
	final long time;
	final double velocity;
	
	private double distanceTravelled;
	
	public Bullet(Point2D origin, double power, long time) {
		this.origin = origin;
		this.power = power;
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
        return (20.0 - (3.0 * power));
    }
    
    public double getDistanceTravelled() {
    	return distanceTravelled;
    }

	public abstract void draw(Graphics2D g);
	
	public abstract boolean shouldDelete();
	
    public abstract void updateProjection();
	
}
