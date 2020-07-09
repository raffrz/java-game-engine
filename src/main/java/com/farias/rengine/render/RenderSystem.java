package com.farias.rengine.render;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.opengl.GL;

import com.farias.rengine.Component;
import com.farias.rengine.Game;
import com.farias.rengine.GameEngine;
import com.farias.rengine.GameObject;
import com.farias.rengine.Transform;
import com.farias.rengine.gfx.Sprite;
import com.farias.rengine.gfx.TileMap;

public class RenderSystem extends com.farias.rengine.System {
	
	//TODO Refactor
	private Camera camera;
	
	public RenderSystem(Game game) {
		super(game);
	}
	
	public void init() {
		camera = new Camera(GameEngine.getWindowWidth(), GameEngine.getWindowHeight());
		//GameEngine.getGameInstance().addEntity(camera);
		GL.createCapabilities();
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_TEXTURE_2D);		
	}

	public void update(long deltaTime) {
		//clears all the pixel colors to black you can use glClearColor to change the
		// clear color
		glClear(GL_COLOR_BUFFER_BIT);
		
		camera.onUpdate(deltaTime);
		
		//TODO Interface Renderable vai para o componente e criar uma interface visible para o game object 
		for (GameObject e : game.getEntities()) {
			if (e instanceof Renderable) {
				Transform transform = (Transform) e.getComponent("transform");
				if (transform == null) {
					continue;
				}
				for (Component c : e.getComponents()) {
					//render sprite
					if (c instanceof Sprite) {
						int sampler = 0;
						((Sprite)c).draw(camera, transform, sampler);
					} else if (c instanceof TileMap) {
						int sampler = 0;
						((TileMap)c).draw(camera, transform, sampler);
					}
				}
			}
		}
	}

	public Camera getCamera() {
		return camera;
	}
}
