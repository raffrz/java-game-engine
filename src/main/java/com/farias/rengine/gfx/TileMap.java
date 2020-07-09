package com.farias.rengine.gfx;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.farias.rengine.Component;
import com.farias.rengine.Transform;
import com.farias.rengine.render.Camera;

public class TileMap extends Component {
	private boolean visible;
	int width;
	int height;
	float scale;
	private Sprite sprite;
	
	public TileMap(Sprite sprite, int width, int height, float scale) {
		this.sprite = sprite;
		this.width = width;
		this.height = height;
		this.scale = scale;
	}
	
	@Override
	public void onInit() {
		sprite.onInit();
		this.show();
	}
	
	@Override
	public void update(long deltaTime) {
	}
	
	public void draw(Camera camera, Transform transform, int sampler) {
		if (!visible) {
			return;
		}
		sprite.getShader().bind();
		sprite.getTexture().bind(sampler);
		sprite.getShader().setUniform("sampler", 0);
		Matrix4f cameraProjection = camera.getProjection();
		for (int i = 0; i < 64; i++) {
			for (int j = 0; j < 64; j++) {
				Matrix4f projection = cameraProjection.scale(transform.getScale(), new Matrix4f());
				projection.translate(new Vector3f(j, i, 0));
				sprite.getShader().setUniform("projection", projection);
				sprite.getModel().draw();
				//sprite.draw(projection, sampler);
			}
		}
	}
	
	public void show() {
		this.visible = true;
	}
	
	public void hide() {
		this.visible = false;
	}
}
