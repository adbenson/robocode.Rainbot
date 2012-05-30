import java.util.LinkedList;

import robocode.ScannedRobotEvent;

class OpponentHistory extends LinkedList<Opponent> {
	final static int MAX_CAPACITY = 100;

	Opponent lastChange = null;

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