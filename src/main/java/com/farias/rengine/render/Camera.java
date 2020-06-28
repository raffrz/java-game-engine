package com.farias.rengine.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.farias.rengine.GameObject;
import com.farias.rengine.Transform;
import com.farias.rengine.gfx.Velocity;

public class Camera extends GameObject {
	
	private static final float MAX_LAG = 32f;
	private Vector3f position;
	private Matrix4f projection;
	private GameObject target;
	private int width;
	private int height;
	private long passed;
	private long passedx;
	private float lagx;
	
	public Camera(int width, int height) {
		this.width = width;
		this.height = height;
		position = new Vector3f(0, 0, 0);
		//usint Matrix4f.ortho2D creates two matrices and multiply then
		//using Matrix4f.setOrtho2D only creates one matrix
		projection = new Matrix4f().setOrtho2D(-width/2, width/2, -height/2, height/2);
	}
	
	@Override
	public void onUpdate(long deltaTime) {
		if (target != null) {
			Velocity velocity = (Velocity) target.getComponent("velocity");
			if (velocity.getVx() == 0) {
				passedx = 0;
				//lagx = 0;
			} else {
				passedx += deltaTime;
				lagx += velocity.getVx() * 0.5f * -1;
			}
			if (lagx > MAX_LAG) {
				lagx = MAX_LAG;
			} else if (lagx < -MAX_LAG) {
				lagx = -MAX_LAG;
			}
		}
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
	
	public void setTarget(GameObject target) {
		this.target = target;
	}
	
	public Matrix4f getProjection() {
		Matrix4f result = new Matrix4f();
		Matrix4f pos = new Matrix4f().setTranslation(position);
		
		if (target != null) {
			Transform transform = (Transform) target.getComponent("transform");
			float tx = transform.getPosition().x * transform.getScale().x;
			float ty = transform.getPosition().y * transform.getScale().y;
			projection = new Matrix4f().setOrtho2D(tx + lagx - width/2, tx + lagx + width/2,
					ty - height/2.0f, ty + height/2.0f);
		}
		result = projection.mul(pos, result);
		return result;
	}
}
