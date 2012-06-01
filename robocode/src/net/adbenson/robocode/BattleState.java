package net.adbenson.robocode;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

public class BattleState {
	
	public final SelfState self;
	public final OpponentState opponent;
	public final long time;
	
	public BattleState(SelfState self, OpponentState opp, long time) {
		this.self = self;
		this.opponent = opp;
		this.time = time;
	}

	public BattleState(AdvancedRobot self, ScannedRobotEvent opp, long time) {
		this(new SelfState(self), new OpponentState(opp, self), time);
	}
	
	public BattleState nextBattleState(AdvancedRobot self, ScannedRobotEvent opp, long time) {
		return new BattleState(
				new SelfState(self, this.self),
				new OpponentState(opp, this.opponent, self),
				time
		);
	}
	
}
