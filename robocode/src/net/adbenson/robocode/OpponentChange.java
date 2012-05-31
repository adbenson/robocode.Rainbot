package net.adbenson.robocode;


@SuppressWarnings("serial")
public class OpponentChange extends OpponentState {

	public OpponentChange(OpponentState previous, OpponentState current) {
		super(
				current.getName() + " change", 
				previous.getEnergy() - current.getEnergy(),
				previous.getBearing() - current.getBearingRadians(),
				previous.getDistance() - current.getDistance(),
				previous.getHeading() - current.getEnergy(),
				previous.getVelocity() - current.getVelocity()
		);
	}
	
	public OpponentChange() {
		super("INITIAL STATE, NO CHANGE", 0,0,0,0,0);
	}

}
