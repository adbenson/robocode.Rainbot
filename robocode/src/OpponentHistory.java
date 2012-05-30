import java.util.LinkedList;

import robocode.ScannedRobotEvent;

@SuppressWarnings("serial")
class OpponentHistory extends LinkedList<Opponent> {
	final static int MAX_CAPACITY = 1000;

	Opponent lastChange = null;
	
	BulletQueue bullets;
	
	public OpponentHistory() {
		super();
		
		bullets = new BulletQueue();
	}

	public boolean add(ScannedRobotEvent e) {
		return this.add(new Opponent(e));
	}

	public boolean add(Opponent o) {
		while (this.size() > MAX_CAPACITY) {
			this.removeFirst();
		}

		if (!this.isEmpty()) {
			lastChange = o.diff(this.getLast());
		}

		return super.add(o);
	}
}