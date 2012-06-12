package net.adbenson.robocode.bullet;
import java.awt.Graphics2D;
import java.util.LinkedList;



@SuppressWarnings("serial")
public class BulletQueue<T extends Bullet> extends LinkedList<T>{
	
	private LinkedList<T> active;
	
	public BulletQueue() {
		super();
		
		active = new LinkedList<T>();
	}
	
	public boolean add(T bullet) {
		active.add(bullet);
		
		return super.add(bullet);
	}

	public void draw(Graphics2D g) {
		for(Bullet b : active) {
			b.draw(g);
		}
	}

	public void updateAll(long time) {
		LinkedList<T> delete = new LinkedList<T>();
		
		for(T b : active) {
			b.updateDistance(time);
			if (b.shouldDelete()) {
				delete.add(b);
				System.out.println("Bullet marked for deletion");				
			}
		}
		
		active.removeAll(delete);
	}
	
	public void terminate(robocode.Bullet bullet, Bullet.Fate fate) {
		Bullet match = null;
		for(Bullet b : this) {
			if (b.matches(bullet)) {
				match = b;
				break;
			}
		}
		
		if (match != null) {
			match.terminate(bullet, fate);
			active.remove(match);			
		}
		else {
			System.out.println("Could not match bullet to remove.");
		}
	}
}
