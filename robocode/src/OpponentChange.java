

@SuppressWarnings("serial")
public class OpponentChange extends Opponent {

	public OpponentChange(OpponentState previousState, OpponentState currentState) {
		super(
				previousState.getName() + " change", 
				currentState.getEnergy() - previousState.getEnergy(),
				currentState.getBearing() - previousState.getBearingRadians(),
				currentState.getDistance() - previousState.getDistance(),
				currentState.getHeading() - previousState.getEnergy(),
				currentState.getVelocity() - previousState.getVelocity()
		);
	}

}
