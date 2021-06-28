package com.farias.rengine.examples;

import com.farias.rengine.ecs.GameObject;
import com.farias.rengine.ecs.Transform;
import com.farias.rengine.ecs.render.Camera;

import org.joml.Matrix4f;

public class TopDownCamera2D extends Camera {
	
	private GameObject target;
	// private float maxLag;
	private float lagx;
	private float lagy;
	
	public TopDownCamera2D(int width, int height, float lag) {
		super(width, height);
		// this.maxLag = lag;
		//usint Matrix4f.ortho2D creates two matrices and multiply then
		//using Matrix4f.setOrtho2D only creates one matrix
		projection = new Matrix4f()
//				.setOrtho2D(-width/2, width/2, -height/2, height/2);
				.setOrtho(-width/2, width/2, -height/2, height/2, -width, width);
	}
	
	@Override
	public void onUpdate(float deltaTime) {
		if (target != null) {
			Transform transform = (Transform) target.getComponent("transform");
//			Velocity velocity = (Velocity) target.getComponent("velocity");
			
			//TODO Implement new camera follow and lag solution, this solution causes glitches
			
//			if (velocity.getVx() != 0) {
//				float factor = 0.2f;
//				if ((velocity.getVx() < 0 && lagx < 0) || (velocity.getVx() > 0 && lagx > 0)) {
//					factor = 1f;
//				}
//				lagx += velocity.getVx() * factor * -1 * deltaTime;
//			}
//			if (lagx > maxLag) {
//				lagx = maxLag;
//			} else if (lagx < -maxLag) {
//				lagx = -maxLag;
//			}
//			
//			if (velocity.getVy() != 0) {
//				float factor = 0.2f;
//				if ((velocity.getVy() < 0 && lagy < 0) || (velocity.getVy() > 0 && lagy > 0)) {
//					factor = 1f;
//				}
//				lagy += velocity.getVy() * factor * -1;
//			}
//			if (lagy > maxLag) {
//				lagy = maxLag;
//			} else if (lagy < -maxLag) {
//				lagy = -maxLag;
//			}
			
			final float tx = transform.getPosition().x * transform.getScale().x;
			final float ty = transform.getPosition().y * transform.getScale().y; 
			projection.setOrtho(tx + lagx - width/2, tx + lagx + width/2,
					ty + lagy - height/2, ty + lagy + height/2, -width, width);
		}
	}
	
	@Override
	public Matrix4f getProjection() {
		return projection.translate(position, new Matrix4f());
//				.rotateX((float) Math.toRadians(-45f))
//				.rotateZ((float) Math.toRadians(-30f));
	}
	
	public GameObject getTarget() {
		return target;
	}
	
	public void setTarget(GameObject target) {
		this.target = target;
	}
}
