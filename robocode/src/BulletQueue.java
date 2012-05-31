import java.awt.Graphics2D;
import java.util.LinkedList;


@SuppressWarnings("serial")
public class BulletQueue extends LinkedList<Bullet>{

	public void draw(Graphics2D g) {
		for(Bullet b : this) {
			b.draw(g);
		}
	}

	public void updateAll(long time) {
		LinkedList<Bullet> delete = new LinkedList<Bullet>();
		
		for(Bullet b : this) {
			b.updateDistance(time);
			if (b.shouldDelete()) {
				delete.add(b);
System.out.println("Bullet marked for deletion");				
			}
		}
		
		this.removeAll(delete);
	}
}
