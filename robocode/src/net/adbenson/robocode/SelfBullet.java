package net.adbenson.robocode;
import java.awt.Graphics2D;

import net.adbenson.utility.Vector;


public class SelfBullet extends Bullet {
	
	private final double direction;

	public SelfBullet(Vector origin, double direction, double power, long time) {
		super(origin, power, time);
		this.direction = direction;
	}

	@Override
	public void draw(Graphics2D g) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean shouldDelete() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void updateProjection() {
		// TODO Auto-generated method stub
		
	}

}
