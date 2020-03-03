package com.farias.rengine.gfx;

import com.farias.rengine.Component;
import com.farias.rengine.io.Controllable;

public class Velocity extends Component {
	float vx;
	float vy;
	
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