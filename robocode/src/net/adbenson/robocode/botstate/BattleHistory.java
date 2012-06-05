package net.adbenson.robocode.botstate;
import java.util.LinkedList;

import net.adbenson.robocode.bullet.BulletQueue;
import net.adbenson.robocode.bullet.OpponentBullet;
import net.adbenson.robocode.bullet.SelfBullet;
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
	
	public boolean addBots(AdvancedRobot self, ScannedRobotEvent opp, long time) {
		BattleState next;
		
		if (getCurrentState() != null) {
			next = getCurrentState().nextBattleState(self, opp, time);
		}
		else {
			next = new BattleState(self, opp, time);
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

	public BulletQueue<SelfBullet> getSelfBullets() {
		return selfBullets;
	}

	public BulletQueue<OpponentBullet> getOpponentBullets() {
		return opponentBullets;
	}

	public BattleState getCurrentState() {
		return currentState;
	}
	
	public OpponentState getCurrentOpponent() {
		return currentState.opponent;
	}

	public void opponentFired() {
		opponentBullets.add(
				new OpponentBullet(
						currentState.opponent,
						currentState.time
				)
		);
	}

	public void selfFired(OpponentState candidateTarget, robocode.Bullet bullet) {
		selfBullets.add(
				new SelfBullet(
						currentState.self,
						bullet,
						currentState.time,
						candidateTarget
				)
		);
	}
}