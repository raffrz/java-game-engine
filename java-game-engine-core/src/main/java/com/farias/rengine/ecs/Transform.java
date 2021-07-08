package com.farias.rengine.ecs;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Transform extends Component {
	Vector3f position;
	Vector3f scale;
	Vector3f rotation = new Vector3f();
	
	public Transform(float x, float y) {
		this.position = new Vector3f();
		this.scale = new Vector3f(1, 1, 1);
	}
	
	@Override
	public void update(float deltaTime) {
		Velocity v = (Velocity) getGameObject().getComponent("velocity");
		if (v != null) {
			// Por que precisa da divisão por 32 para ajuster movimento da câmera?
			position.x += v.getVx() / scale.x;
			position.y += v.getVy() / scale.y;
		}
	}
	
	public void setPosition(Vector3f position) {
		this.position = position;
	}
	
	public Vector3f getPosition() {
		return position;
	}
	
	public Vector3f getRotation() {
		return rotation;
	}
	
	public void setRotation(Vector3f rotation) {
		this.rotation = rotation;
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
		target.rotateXYZ(rotation);
		
		return target;
	}
}
