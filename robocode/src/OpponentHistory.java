import java.util.LinkedList;

import robocode.ScannedRobotEvent;

@SuppressWarnings("serial")
public class OpponentHistory extends LinkedList<OpponentState> {
	final static int MAX_CAPACITY = 1000;
	
	BulletQueue bullets;
	
	OpponentState last;
	
	public OpponentHistory() {
		super();
		
		bullets = new BulletQueue();
	}

	public boolean add(ScannedRobotEvent e) {
		return this.add(new OpponentState(e, this.getLast()));
	}

	public boolean add(OpponentState o) {
		while (this.size() > MAX_CAPACITY) {
			this.removeFirst();
		}
		
		this.last = o;
		
		return super.add(o);
	}
	
	public Opponent getLastChange() {
		if (this.last != null) {
			return this.last.change;
		}
		else {
			return null;
		}
	}
}