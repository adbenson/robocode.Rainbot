package net.adbenson.robocode.bullet;
import java.awt.Graphics2D;
import java.util.LinkedList;



@SuppressWarnings("serial")
public class BulletQueue<T extends Bullet> extends LinkedList<T>{

	public void draw(Graphics2D g) {
		for(Bullet b : this) {
			b.draw(g);
		}
	}

	public void updateAll(long time) {
		LinkedList<T> delete = new LinkedList<T>();
		
		for(T b : this) {
			b.updateDistance(time);
			if (b.shouldDelete()) {
				delete.add(b);
System.out.println("Bullet marked for deletion");				
			}
		}
		
		this.removeAll(delete);
	}
	
	public void remove(robocode.Bullet bullet) {
		//TODO
	}
}
