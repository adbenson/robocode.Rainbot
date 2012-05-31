package net.adbenson.robocode;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;


public class OpponentBullet extends Bullet {
	
	private Ellipse2D radius;
	private double bearing;

	public OpponentBullet(Point2D origin, double bearing, double power, long time) {
		super(origin, power, time);
		this.bearing = bearing;
	}
	
	public Ellipse2D getBulletRadius() {
		return radius;
	}

	@Override
	public void draw(Graphics2D g) {
		Color c = Color.white;
		g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 50));
		g.setStroke(new BasicStroke(3));
		g.draw(radius);

		double endX = Math.sin(bearing) * getDistanceTravelled();
		double endY = Math.cos(bearing) * getDistanceTravelled();
//System.out.println(bearing);		
		g.drawLine((int)origin.getX(), (int)origin.getY(), (int)(origin.getX()+endX), (int)(origin.getY()+endY));
		
	}

	@Override
	public boolean shouldDelete() {
		return radius.contains(Rainbot.getField());
	}

	@Override
	public void updateProjection() {
		double radius = getDistanceTravelled();
		this.radius = new Ellipse2D.Double(origin.getX()-radius, origin.getY()-radius, radius*2, radius*2);
	}


}
