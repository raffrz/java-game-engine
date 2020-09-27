package com.farias.rengine.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.farias.rengine.GameObject;

public abstract class Camera extends GameObject {
	protected Vector3f position;
	protected Matrix4f projection;
	protected int width;
	protected int height;
	
	public Camera(int width, int height) {
		this.width = width;
		this.height = height;
		position = new Vector3f(0, 0, 0);
	}
	
	@Override
	public void onUpdate(float deltaTime) {

	}
	
	public void setPosition(Vector3f position) {
		this.position = position;
	}
	
	public void addPosition(Vector3f position) {
		this.position.add(position);
	}
	
	public Vector3f getPosition() {
		return position;
	}
	
	public abstract Matrix4f getProjection();
}
