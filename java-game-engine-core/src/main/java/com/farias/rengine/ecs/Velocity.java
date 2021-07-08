package com.farias.rengine.ecs;

public class Velocity extends Component {
	float vx;
	float vy;
	
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