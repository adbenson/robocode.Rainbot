package net.adbenson.robocode.bullet;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import net.adbenson.robocode.botstate.OpponentState;
import net.adbenson.robocode.botstate.SelfState;
import net.adbenson.robocode.rainbot.Rainbot;
import net.adbenson.utility.Utility;
import net.adbenson.utility.Vector;
import robocode.Rules;


public class OpponentBullet extends Bullet {
	
	private static final double TIME_FUDGE = 2.1;
	
	private Ellipse2D radius;

	public OpponentBullet(OpponentState opponent, SelfState self, long time) {
		super(opponent, self, Utility.oppositeAngle(opponent.absoluteBearing), time);
	}

	public Ellipse2D getBulletRadius() {
		return radius;
	}
	
	public double calculateDistanceTravelled(long time) {
		return velocity * (time + TIME_FUDGE);
	}

	@Override
	public void draw(Graphics2D g) {
		drawEscapePoints(g, Color.yellow);
		
		g.setColor(Utility.setAlpha(Color.white, 0.25));
		g.draw(radius);

		origin.drawTo(g, heading, getDistanceTravelled());
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
