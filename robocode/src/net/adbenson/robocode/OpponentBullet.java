package net.adbenson.robocode;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import net.adbenson.utility.Vector;


public class OpponentBullet extends Bullet {
	
	private Ellipse2D radius;

	public OpponentBullet(OpponentState opponent, double heading, long time) {
		super(opponent, heading, time);
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

		double endX = Math.sin(heading) * getDistanceTravelled();
		double endY = Math.cos(heading) * getDistanceTravelled();

		g.drawLine(origin.intX(), origin.intY(), (int)(origin.x+endX), (int)(origin.y + endY));
		
	}

	@Override
	public boolean shouldDelete() {
		return radius.contains(Rainbot.getField());
	}

	@Override
	public void updateProjection() {
		double radius = getDistanceTravelled();
		this.radius = new Ellipse2D.Double(origin.x - radius, origin.y - radius, radius*2, radius*2);
	}


}
