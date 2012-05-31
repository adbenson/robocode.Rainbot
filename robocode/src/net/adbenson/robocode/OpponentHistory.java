package net.adbenson.robocode;
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
		
		last = null;
	}

	public boolean add(ScannedRobotEvent e) {
		
		while (this.size() > MAX_CAPACITY) {
			this.removeFirst();
		}
		
		OpponentState current = new OpponentState(e, last);
		
		this.last = current;
		
		return super.add(current);
	}
}