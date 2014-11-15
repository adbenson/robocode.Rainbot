package net.adbenson.robocode;

import robocode.AdvancedRobot;

public abstract class TurnBot extends AdvancedRobot {
	
	@Override
	public final void run() {
		init();
		while(true) {
			turn();
			super.execute();
		}
	}
	
	public abstract void init();
	
	public abstract void turn();
	
	@Override
	public final void ahead(double x) {
		throw new DisallowedActionException("ahead");
	}
	@Override
	public final void back(double x) {
		throw new DisallowedActionException("back");
	}
	
	@Override
	public final void turnLeft(double x) {
		throw new DisallowedActionException("turnLeft");
	}
	@Override
	public final void turnRight(double x) {
		throw new DisallowedActionException("turnRight");
	}
	
	@Override
	public final void fire(double x) {
		throw new DisallowedActionException("fire");
	}
	@Override
	public final robocode.Bullet fireBullet(double x) {
		throw new DisallowedActionException("fireBullet");
	}
	
	@Override
	public final void turnGunLeft(double x) {
		throw new DisallowedActionException("turnGunLeft");
	}
	@Override
	public final void turnGunRight(double x) {
		throw new DisallowedActionException("turnGunRight");
	}
	
	@Override
	public final void turnRadarLeft(double x) {
		throw new DisallowedActionException("turnRadarLeft");
	}
	@Override
	public final void turnRadarRight(double x) {
		throw new DisallowedActionException("turnRadarRight");
	}
	
	@Override
	public final void execute() {
		throw new DisallowedActionException("execute");
	}
	
	@SuppressWarnings("serial")
	public static class DisallowedActionException extends RuntimeException {
		DisallowedActionException(String called) {
			super("Method "+called+" is not allowed as it would cause premature end-of-turn. Use "+expected(called)+" instead.");
		}
		private static String expected(String called) {
			return called.substring(0, 1).toUpperCase() + called.substring(1);
		}
	}
}
