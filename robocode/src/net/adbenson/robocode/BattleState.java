package net.adbenson.robocode;
import net.adbenson.utility.Vector;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;


@SuppressWarnings("serial")
public class BattleState {
	
	final SelfState self;
	final OpponentState opponent;
	
	public BattleState(SelfState self, OpponentState opp) {
		this.self = self;
		this.opponent = opp;
	}

	public BattleState(AdvancedRobot self, ScannedRobotEvent opp) {
		this(new SelfState(self), new OpponentState(opp));
	}
	
	public BattleState nextBattleState(AdvancedRobot self, ScannedRobotEvent opp) {
		return new BattleState(
				new SelfState(self, this.self),
				new OpponentState(opp, this.opponent)
		);
	}
	
}
