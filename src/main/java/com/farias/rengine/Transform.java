package com.farias.rengine;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import com.farias.rengine.gfx.Velocity;

public class Transform extends Component {
	Vector2f position;
	Vector2f rotation = new Vector2f();
	Matrix4f scale;
	
	public Transform(float x, float y) {
		this.position = new Vector2f(x / 100, y / 100);
		this.scale = new Matrix4f()
				.translate(new Vector3f(0, 0, 0))
				.scale(128);
	}
	
	@Override
	public void update(long deltaTime) {
		Velocity v = getGameObject().getComponent(Velocity.class);
		position.x += v.getVx() / 100;
		position.y += v.getVy() / 100;
	}
	
	public Vector2f getPosition() {
		return position;
	}
	
	public Vector2f getRotation() {
		return rotation;
	}
	
	public Matrix4f getScale() {
		return scale;
	}
}
