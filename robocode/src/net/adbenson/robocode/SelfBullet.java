package net.adbenson.robocode;
import java.awt.Graphics2D;


public class SelfBullet extends Bullet {
	
	public SelfBullet(SelfState self, long time) {
		super(self, self.gunHeading, time);
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
