package com.farias.rengine.animation;

import java.util.List;
import java.util.Map;

import com.farias.rengine.ecs.Component;
import com.farias.rengine.GameEngine;

public class Animation extends Component {
	private long passed;
	private int frame;
	private List<Map<String, Object>> frameData;
	private Animatable target;
	
	public Animation(Animatable target) {
		this.target = target;
	}
	
	public void update(long deltaTime) {
		passed += deltaTime;
		if (passed >= 1000 / 60 * 20) {
			nextFrame();
			passed = 0;
		}
	}

	public void registerFrame(Map<String, Object> properties) {
		frameData.add(properties);
	}
	
	private void nextFrame() {
		frame = ++frame > frameData.size() ? 0 : frame;
		target.setProperties(frameData.get(frame));
		GameEngine.getEventSystem().triggerEvent("frameChange");
	}
}
