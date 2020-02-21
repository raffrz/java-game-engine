package com.farias.rengine.render;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.opengl.GL;

import com.farias.rengine.Game;
import com.farias.rengine.GameEngine;
import com.farias.rengine.GameObject;
import com.farias.rengine.Transform;
import com.farias.rengine.gfx.Sprite;

public class RenderSystem extends com.farias.rengine.System {
	
	//TODO Refactor
	private Camera camera;
	
	public RenderSystem(Game game) {
		super(game);
	}
	
	public void init() {
		camera = new Camera(GameEngine.getWindowWidth(), GameEngine.getWindowHeight());
		GL.createCapabilities();
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_TEXTURE_2D);		
	}

	public void update(long deltaTime) {
		//clears all the pixel colors to black you can use glClearColor to change the
		// clear color
		glClear(GL_COLOR_BUFFER_BIT);
		
		for (GameObject e : game.getEntities()) {
			if (e instanceof Renderable) {
				Transform transform = e.getComponent(Transform.class);
				Sprite sprite = e.getComponent(Sprite.class);
				//render sprites
				if (transform != null && sprite != null) {
					this.render(transform, sprite);
				}
			}
		}
	}
	
	public void render(Transform transform, Sprite sprite) {
		int sampler = 0;
		sprite.draw(camera, transform, sampler);
	}
}
