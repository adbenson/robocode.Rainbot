package net.adbenson.robocode.rainbot;

import java.awt.Color;

import net.adbenson.robocode.trigger.Trigger;

import robocode.Robot;

public class BotColor {
	
	private float hue;
	
	private static final float HUE_SHIFT = 0.003f;
	private static final int RAINBOW_TURNS = 10;
	
	private boolean rainbowMode;
	private float rainbowTurn;
	
	public BotColor() {
		hue = 0;
		rainbowMode = false;
	}
	
	public void hueShift(Robot bot) {
		
		if (rainbowMode) {
			hueJump();
			rainbowTurn++;
			rainbowMode = (rainbowTurn <= RAINBOW_TURNS);	
		}
		
		float shift = HUE_SHIFT * (rainbowMode? 3 : 1);
		hue = (hue + shift) % 1.0f;
		bot.setColors(
				Color.getHSBColor(hue, 1, 1),
				Color.getHSBColor(hue + 0.25f, 1, 1),
				Color.getHSBColor(hue + 0.7f, 1, 1),
				Color.orange, //It's just confusing if you can't see the bullet.
				Color.getHSBColor(hue + 0.75f, 1, 1)
		);
	}
	
	private void hueJump() {
		hue = (hue + 0.3f) % 1.0f;
	}
	
	public void startRainbow() {
		rainbowMode = true;
		rainbowTurn = 0;
	}
}
