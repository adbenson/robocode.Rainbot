package wiki.tutorial.gftargeting;

import java.awt.geom.Point2D;
import java.util.LinkedList;

import robocode.util.Utils;

public class WaveManager extends LinkedList<Wave>{
	
	static Point2D targetLocation;
	
	public static final double MAX_DISTANCE = 900;
	public static final int DISTANCE_INDEXES = 5;
	public static final int VELOCITY_INDEXES = 5;
	public static final int BINS = 25;
	public static final int MIDDLE_BIN = (BINS - 1) / 2;
	public static final double MAX_ESCAPE_ANGLE = 0.7;
	public static final double BIN_WIDTH = MAX_ESCAPE_ANGLE / (double)MIDDLE_BIN;
	
	private static int[][][][] statBuffers = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES][BINS];
	
	private int mostVisitedBin() {
		int mostVisited = MIDDLE_BIN;
		for (int i = 0; i < BINS; i++) {
			if (buffer[i] > buffer[mostVisited]) {
				mostVisited = i;
			}
		}
		return mostVisited;
	}	

	private double mostVisitedBearingOffset() {
		return (lateralDirection * BIN_WIDTH) * (mostVisitedBin() - MIDDLE_BIN);
	}


	public boolean done() {
		advance();
		if (hasArrived()) {
			buffer[currentBin()]++;
			robot.removeCustomEvent(this);
		}
		return false;
	}

	public void advanceAll() {
		// TODO Auto-generated method stub
		
	}


}
