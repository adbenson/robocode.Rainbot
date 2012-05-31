package net.adbenson.robocode;
import java.util.LinkedList;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

@SuppressWarnings("serial")
public class BattleHistory extends LinkedList<BattleState> {
	final static int MAX_CAPACITY = 1000;
	
	BulletQueue<OpponentBullet> opponentBullets;
	BulletQueue<SelfBullet> selfBullets;
	
	BattleState current;
	
	public BattleHistory() {
		super();
		
		opponentBullets = new BulletQueue<OpponentBullet>();
		selfBullets = new BulletQueue<SelfBullet>();
		
		current = null;
	}
	
	public boolean addBots(ScannedRobotEvent opp, AdvancedRobot self) {
		BattleState next;
		
		if (current != null) {
			next = current.nextBattleState(self, opp);
		}
		else {
			next = new BattleState(self, opp);
		}
		
		return this.add(next);
	}

	public boolean add(BattleState state) {
		
		while (this.size() > MAX_CAPACITY) {
			this.removeFirst();
		}
		
		this.current = state;
		
		return super.add(current);
	}
}