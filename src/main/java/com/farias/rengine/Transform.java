package com.farias.rengine;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import com.farias.rengine.gfx.Velocity;

public class Transform extends Component {
	Vector3f position;
	Vector3f scale;
	Vector2f rotation = new Vector2f();
	
	public Transform(float x, float y) {
		this.position = new Vector3f();
		this.scale = new Vector3f(1, 1, 1);
	}
	
	@Override
	public void update(long deltaTime) {
		Velocity v = (Velocity) getGameObject().getComponent("velocity");
		position.x += v.getVx() / 100;
		position.y += v.getVy() / 100;
	}
	
	public Vector3f getPosition() {
		return position;
	}
	
	public Vector2f getRotation() {
		return rotation;
	}
	
	public Vector3f getScale() {
		return scale;
	}
	
	public void setScale(Vector3f scale) {
		this.scale = scale;
	}
	
	public Matrix4f getProjection(Matrix4f target) {
		target.scale(scale);
		target.translate(position);
		return target;
	}
}
