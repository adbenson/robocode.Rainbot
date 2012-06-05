package net.adbenson.robocode.bullet;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import net.adbenson.robocode.botstate.OpponentState;
import net.adbenson.robocode.botstate.SelfState;
import net.adbenson.robocode.rainbot.Rainbot;
import net.adbenson.utility.Utility;


public class SelfBullet extends Bullet {
	
	private final OpponentState target;
	
	public SelfBullet(SelfState self, long time, OpponentState target) {
		super(self, self.gunHeading, time);
		this.target = target;
	}

	@Override
	public void draw(Graphics2D g) {
		g.setStroke(new BasicStroke(2));
		g.setColor(Color.orange);
		Utility.drawCrosshairs(g, target.position, 5, 40);
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
