package net.adbenson.robocode;
import java.util.LinkedList;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

@SuppressWarnings("serial")
public class BattleHistory extends LinkedList<BattleState> {
	private final static int MAX_CAPACITY = 1000;
	
	private BulletQueue<OpponentBullet> opponentBullets;
	private BulletQueue<SelfBullet> selfBullets;
	
	private BattleState currentState;
	
	public BattleHistory() {
		super();
		
		opponentBullets = new BulletQueue<OpponentBullet>();
		selfBullets = new BulletQueue<SelfBullet>();
		
		currentState = null;
	}
	
	public boolean addBots(AdvancedRobot self, ScannedRobotEvent opp) {
		BattleState next;
		
		if (getCurrentState() != null) {
			next = getCurrentState().nextBattleState(self, opp);
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
		
		currentState = state;
		
		return super.add(getCurrentState());
	}

	BulletQueue<SelfBullet> getSelfBullets() {
		return selfBullets;
	}

	BulletQueue<OpponentBullet> getOpponentBullets() {
		return opponentBullets;
	}

	BattleState getCurrentState() {
		return currentState;
	}
}