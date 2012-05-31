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
		distanceTravelled = velocity * timeElapsed;
	}
	
    private double velocity() {
        return (20.0 - (3.0 * power));
    }
	
}
