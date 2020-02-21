package com.farias.rengine.gfx;

import com.farias.rengine.Component;
import com.farias.rengine.io.Controllable;

public class Velocity extends Component {
	float vx;
	float vy;
	
	@Override
	public void handleEvent(String event) {
		if (!(getGameObject() instanceof Controllable)) {
			return;
		}
		if (event.contentEquals("up_pressed")) {
			vy = 1;
		}
		if (event.contentEquals("down_pressed")) {
			vy = -1;
		} 
		if (event.contentEquals("left_pressed")) {
			vx = -1;
		}
		if (event.contentEquals("right_pressed")) {
			vx = 1;
		}
		if (event.contentEquals("up_released") ||
				event.contentEquals("down_released")) {
			vy = 0;
		}
		if (event.contentEquals("left_released") || 
				event.contentEquals("right_released")) {
			vx = 0;
		}
	}
	
	@Override
	public void update(long deltaTime) {

	}
	
	public float getVx() {
		return vx;
	}
	
	public float getVy() {
		return vy;
	}
	
	public void setVx(float vx) {
		this.vx = vx;
	}
	
	public void setVy(float vy) {
		this.vy = vy;
	}
}