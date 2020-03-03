package com.farias.rengine.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.farias.rengine.GameObject;
import com.farias.rengine.Transform;

public class Camera extends GameObject {
	
	private static final float MAX_DISTANCE = 1.5f;
	private Vector3f position;
	private Matrix4f projection;
	private GameObject target;
	float offsetX;
	
	public Camera(int width, int height) {
		position = new Vector3f(0, 0, 0);
		//usint Matrix4f.ortho2D creates two matrices and multiply then
		//using Matrix4f.setOrtho2D only creates one matrix
		projection = new Matrix4f().setOrtho2D(-width/2, width/2, -height/2, height/2);
	}
	
	@Override
	public void onUpdate(long deltaTime) {
		if (target == null) {
			return;
		}
		Vector3f targetPosition = ((Transform) target.getComponent("transform")).getPosition();
		System.out.println("player: [" + targetPosition.x + "],[" + targetPosition.y + "]");
		System.out.println("camera: [" + -this.position.x + "],[" + this.position.y + "]");
		
		Vector3f distance = new Vector3f();
		new Vector3f(position.x, position.y, position.z)
			.sub(new Vector3f(targetPosition.x, targetPosition.y, targetPosition.y), distance);
		
		System.out.println("camera distance: [" + (distance.x - offsetX) + "],[" + distance.y + "]");
		if (distance.x < -MAX_DISTANCE) { // -4, -3
			//position.x -= distance.x - MAX_DISTANCE;
		}
		if (distance.x - offsetX > MAX_DISTANCE) {
			//a posição da camera é igual a posição do alvo mais a distancia maxima
//			System.out.println("old camera position: [" + position.x + "],[" + position.y + "]");
//			System.out.println("camera distance: [" + distance.x + "],[" + distance.y + "]");
//			System.out.println("posx=" + targetPosition + " + (" + distance.x + " - " + MAX_DISTANCE + ")");
//			System.out.println("posx=" + position.x);
//			System.out.println("new camera position: [" + position.x + "],[" + position.y + "]");
			this.addPosition(new Vector3f(0.1f, 0, 0));
			//position.x+=0.1f;
			offsetX+=0.1f;
		}
		if (distance.y < -MAX_DISTANCE) {
			//position.y++;
		}
		if (distance.y > MAX_DISTANCE) {
			//position.y--;
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
		Matrix4f target = new Matrix4f();
		Matrix4f pos = new Matrix4f().setTranslation(position);
		
		target = projection.mul(pos, target);
		return target;
	}
}
