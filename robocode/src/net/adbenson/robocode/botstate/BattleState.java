package net.adbenson.robocode.botstate;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import net.adbenson.robocode.bullet.BulletQueue;
import net.adbenson.robocode.bullet.OpponentBullet;
import net.adbenson.robocode.bullet.SelfBullet;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

public class BattleState {
	
	private BulletQueue<SelfBullet> selfBullets;
	private HashMap<String, BulletQueue<OpponentBullet>> opponentBullets;
	
	private SelfState selfState;
	private HashMap<String, OpponentState> opponentStates;
	
	private long turn;
	
	private String targetName;
	
	public BattleState() {
		super();

		selfBullets = new BulletQueue<SelfBullet>();
		opponentBullets = new HashMap<String, BulletQueue<OpponentBullet>>();
		
		selfState = null;
		opponentStates = new HashMap<String, OpponentState>();
		
		turn = -1;
		targetName = "";
	}
	
	public void addBots(AdvancedRobot self, LinkedList<ScannedRobotEvent> foundOpponents) {
		this.turn = self.getTime();
		
		selfState = new SelfState(self, selfState);
		
		for(ScannedRobotEvent opponent : foundOpponents) {
			String name = opponent.getName();
			OpponentState newOpponentState;
			
			if (opponentStates.containsKey(name)) {
				OpponentState currentOpponent = opponentStates.get(name);
				
				newOpponentState = new OpponentState(opponent, currentOpponent, self);
			}
			else {
				newOpponentState = new OpponentState(opponent, self);
			}
			
			opponentStates.put(name, newOpponentState);
		}
	}

	public BulletQueue<SelfBullet> getSelfBullets() {
		return selfBullets;
	}

	public BulletQueue<OpponentBullet> getOpponentBullets(String name) {
		return opponentBullets.get(name);
	}
	
	public OpponentState getOpponent(String name) {
		return opponentStates.get(name);
	}

	public OpponentBullet opponentFired(String name, long turn) {
		OpponentState opponent = opponentStates.get(name);
		
		OpponentBullet bullet = new OpponentBullet(opponent, selfState, turn);
		
		BulletQueue<OpponentBullet> queue;
		
		if (!opponentBullets.containsKey(name)) {
			queue = new BulletQueue<OpponentBullet>();
			opponentBullets.put(name, queue);
		}
		else {
			queue = opponentBullets.get(name);
		}
		
		queue.add(bullet);
		
		return bullet;
	}

	public void selfFired(OpponentState target, robocode.Bullet bullet, long turn) {
		selfBullets.add(new SelfBullet(selfState, target, bullet, turn));
	}

	public Collection<BulletQueue<OpponentBullet>> getAllOpponentBullets() {
		return opponentBullets.values();
	}

	public SelfState getSelf() {
		return selfState;
	}

	public Collection<OpponentState> getAllOpponents() {
		return opponentStates.values();
	}

	public boolean hasTarget() {
		return opponentStates.containsKey(targetName);
	}
	
	public long getTurn() {
		return turn;
	}
	
	public void setTargetName(String name) {
		this.targetName = name;
	}
	
	public String getTargetName() {
		return targetName;
	}
	
	public OpponentState getTarget() {
		return opponentStates.get(targetName);
	}
}