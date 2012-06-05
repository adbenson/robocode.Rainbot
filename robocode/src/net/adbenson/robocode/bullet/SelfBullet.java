package net.adbenson.robocode.bullet;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import net.adbenson.robocode.botstate.OpponentState;
import net.adbenson.robocode.botstate.SelfState;
import net.adbenson.robocode.rainbot.Rainbot;
import net.adbenson.utility.Utility;
import net.adbenson.utility.Vector;


public class SelfBullet extends Bullet {
	
	private static final int COLOR_WHEEL = 5;
	private static final float COLOR_RATIO = 1f / COLOR_WHEEL;
	private static int nextColorIndex = 0;
		
	private final OpponentState target;
	private final int colorIndex;
	
	public SelfBullet(SelfState self, long time, OpponentState target) {
		super(self, self.gunHeading, time);
		this.target = target;
		this.colorIndex = (nextColorIndex++) % COLOR_WHEEL;
	}

	@Override
	public void draw(Graphics2D g) {
		g.setStroke(new BasicStroke(2));

		Color c = Color.getHSBColor(COLOR_RATIO * colorIndex, 1, 1);
		g.setColor(Utility.setAlpha(c, 0.6));
		
		Utility.drawCrosshairs(g, target.position, 5, 40);
		
		Vector current = Vector.getVectorFromAngle(heading, getDistanceTravelled()).add(origin);
		
		origin.drawTo(g, heading, getDistanceTravelled());
		
		g.fillOval(current.intX(), current.intY(), 20, 20);
	}

	@Override
	public boolean shouldDelete() {
		return getDistanceTravelled() > Rainbot.getField().getWidth();
	}

	@Override
	public void updateProjection() {
		// TODO Auto-generated method stub
		
	}

}
