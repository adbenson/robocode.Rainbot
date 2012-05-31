package net.adbenson.robocode;
import java.util.LinkedList;

import robocode.ScannedRobotEvent;

@SuppressWarnings("serial")
public class BattleHistory extends LinkedList<BattleState> {
	final static int MAX_CAPACITY = 1000;
	
	BulletQueue bullets;
	
	BattleState last;
	
	OpponentState currentOpponent;
	
	public BattleHistory() {
		super();
		
		bullets = new BulletQueue();
		
		last = null;
	}

	public boolean add(ScannedRobotEvent e) {
		
		while (this.size() > MAX_CAPACITY) {
			this.removeFirst();
		}
		
		BattleState current = new BattleState(e, last.current);
		
		this.last = current;
		
		return super.add(current);
	}
}