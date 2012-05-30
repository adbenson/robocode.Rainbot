import java.util.HashMap;

import robocode.AdvancedRobot;
import robocode.CustomEvent;


class TriggerSet extends HashMap<String, Trigger> {
	
	public void add(Trigger t) {
		this.put(t.getName(), t);
	}
	
	public void trigger(String name) {
		if (this.containsKey(name))  {
			this.get(name).action();
		}
	}
	
	public void trigger(CustomEvent event) {
		System.out.println("Event triggered: "+event.getCondition().getName());
		this.trigger(event.getCondition().getName());
	}
	
	public void addTo(AdvancedRobot robot) {
		for(Trigger t : this.values()) {
			System.out.println("Adding trigger: "+t.getName());
			robot.addCustomEvent(t);
		}
	}
}