package wiki.tutorial.gftargeting;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

public class WaveManager extends LinkedList<Wave>{
	
	public static final double MAX_DISTANCE = 900;
	public static final int DISTANCE_INDEXES = 5;
	public static final int VELOCITY_INDEXES = 5;
	public static final int BINS = 25;
	public static final int LAST_BIN = BINS - 1;
	public static final int MIDDLE_BIN = LAST_BIN / 2;
	public static final double MAX_ESCAPE_ANGLE = 0.7;
	public static final double BIN_WIDTH = MAX_ESCAPE_ANGLE / (double)MIDDLE_BIN;
	
	//Enemy distance, current velocity, previous velocity, and angle of offset ("guess factor")
//	private static int[][][][] statBuffers = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES][BINS];
	
	private int[] angleOffsets;
	private int mostVisited;
	private int lastVisited;
	
	public WaveManager() {
		angleOffsets = new int[BINS];
		Arrays.fill(angleOffsets, 0);
		
		mostVisited = MIDDLE_BIN;
		lastVisited = MIDDLE_BIN;
	}
	
	public double mostVisitedBearingOffset(int lateralDirection) {
		return (lateralDirection * BIN_WIDTH) * (mostVisited - MIDDLE_BIN);
	}

	public void advanceAll(Point2D currentTargetLocation, int opponentDirection) {
		//Use of an iterator allows removing elements during iteration
		for (Iterator<Wave> i = iterator(); i.hasNext();) {
			Wave wave = i.next();
			wave.advance();
			
			if (wave.hasArrived(currentTargetLocation)) {
				int index = wave.angleOffsetIndex(currentTargetLocation, opponentDirection);
				visitSegment(index);
				
				i.remove();
			}
		}
	}
	
	private void visitSegment(int index) {
		//indicates that this wave would have hit the bot at this offset
		angleOffsets[index]++;
		lastVisited = index;
		
		if (angleOffsets[index] > angleOffsets[mostVisited]) {
			mostVisited = index;
		}
	}

	public void drawAll(Graphics2D g, Point2D currentLocation) {
		Wave oldest = null;
		
		for (Wave wave : this) {
			wave.draw(g);
			
			if (oldest == null || wave.getDistanceTraveled() > oldest.getDistanceTraveled()) {
				drawEscapeSegments(g, currentLocation, wave);
				oldest = wave;
			}
		}
	}
	
	private void drawEscapeSegments(Graphics2D g, Point2D origin, Wave wave) {
		drawEscapeSegments(g, wave.origin, wave.heading, wave.getDistanceTraveled(), mostVisited);
	}
	
	public void drawEscapeSegments(Graphics2D g, Point2D origin, double heading, double distance) {
		drawEscapeSegments(g, origin, heading, distance, mostVisited);
	}

	public void drawEscapeSegments(Graphics2D g, Point2D origin, double bearing, double distance, int highlight) {
		double firstBinAngle = bearing - MAX_ESCAPE_ANGLE;
		
		for (int i = 0; i <= BINS; i++) {
			g.setColor(Color.BLUE);
			
			double angle = firstBinAngle + (BIN_WIDTH * i);
			Point2D outside = TargetingUtils.project(origin, angle, distance + 5);
			Point2D inside = TargetingUtils.project(origin, angle, distance - 5);
			g.drawLine((int)outside.getX(), (int)outside.getY(), (int)inside.getX(), (int)inside.getY());
			
			if (i < BINS) {
				Point2D labels = TargetingUtils.project(origin, angle + (BIN_WIDTH / 2), distance);

				g.setColor(bubbleColor(i));
				g.fillOval((int)labels.getX() - 2, (int)labels.getY() - 2, 9, 12);
				
				g.setColor(labelColor(i));
				g.drawString(Integer.toString(angleOffsets[i]), (int)labels.getX(), (int)labels.getY());
			}
		}
	}

	private Color bubbleColor(int i) {
		if (i == mostVisited) {
			return Color.ORANGE;
		}
		else if (i == lastVisited) {
			return Color.GREEN;
		}
		else {
			return Color.BLUE;
		}
	}
	
	private Color labelColor(int i) {
		if (i == mostVisited) {
			return Color.BLACK;
		}
		else if (i == lastVisited) {
			return Color.BLACK;
		}
		else {
			return Color.WHITE;
		}
	}

	public void drawEscapeMetrics(Graphics2D g, Point2D origin, double bearing, double distance, Point2D enemyLocation) {
		
		g.setColor(Color.WHITE);
		g.drawLine((int)origin.getX(), (int)origin.getY(),(int)enemyLocation.getX(), (int)enemyLocation.getY());
		
		
		g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {10.0f}, 0.0f));
		
		Point2D start = TargetingUtils.project(origin, bearing - MAX_ESCAPE_ANGLE, distance);
		g.drawLine((int)origin.getX(), (int)origin.getY(),(int)start.getX(), (int)start.getY());
		
		Point2D end = TargetingUtils.project(origin, bearing + MAX_ESCAPE_ANGLE, distance);
		g.drawLine((int)origin.getX(), (int)origin.getY(),(int)end.getX(), (int)end.getY());
		
		double firstBinAngle = bearing - MAX_ESCAPE_ANGLE;
		
		for (int i = 0; i < BINS; i++) {
			
			double angle = firstBinAngle + (BIN_WIDTH * i);
			
			Point2D outside = TargetingUtils.project(origin, angle, distance + 5);
			Point2D inside = TargetingUtils.project(origin, angle, distance - 5);
			g.drawLine((int)outside.getX(), (int)outside.getY(), (int)inside.getX(), (int)inside.getY());
				
			
			if (i == 0 || i == LAST_BIN || i == MIDDLE_BIN) {
				Point2D labelPoint = TargetingUtils.project(origin, angle, distance + 15);
				String label = getLabel(i);
				g.drawString(label, (int)labelPoint.getX(), (int)labelPoint.getY());
			}
		}
		
		Point2D midpoint = TargetingUtils.project(origin, bearing, distance / 2);
		g.setFont(new Font("Serif", Font.ITALIC | Font.BOLD, 12));
		g.drawString("d", (int)midpoint.getX() + 10, (int)midpoint.getY());
	}

	private String getLabel(int i) {
		double bin = (i / (LAST_BIN / 2.0)) - 1.0;
		return (bin>0? "+":"") + new DecimalFormat("0.0").format(bin);
	}

}
