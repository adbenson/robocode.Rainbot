package net.adbenson.robocode;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

public class BattleState {
	
	public final SelfState self;
	public final OpponentState opponent;
	
	public BattleState(SelfState self, OpponentState opp) {
		this.self = self;
		this.opponent = opp;
	}

	public BattleState(AdvancedRobot self, ScannedRobotEvent opp) {
		this(new SelfState(self), new OpponentState(opp, self));
	}
	
	public BattleState nextBattleState(AdvancedRobot self, ScannedRobotEvent opp) {
		return new BattleState(
				new SelfState(self, this.self),
				new OpponentState(opp, this.opponent, self)
		);
	}
	
}
