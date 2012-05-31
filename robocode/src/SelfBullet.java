import java.awt.geom.Point2D;


public class SelfBullet extends Bullet {
	
	final double direction;

	public SelfBullet(Point2D origin, double direction, double power, long time) {
		super(origin, power, time);
		this.direction = direction;
	}

}
