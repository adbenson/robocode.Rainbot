package net.adbenson.robocode.bullet;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import net.adbenson.robocode.botstate.OpponentState;
import net.adbenson.robocode.rainbot.Rainbot;
import net.adbenson.utility.Utility;


public class OpponentBullet extends Bullet {
	
	private Ellipse2D radius;

	public OpponentBullet(OpponentState opponent, long time) {
		super(opponent, Utility.oppositeAngle(opponent.absoluteBearing), time);
	}

	public Ellipse2D getBulletRadius() {
		return radius;
	}

	@Override
	public void draw(Graphics2D g) {
		g.setColor(Utility.setAlpha(Color.white, 0.25));
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
