package com.farias.rengine.ecs.render;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.opengl.GL;

import com.farias.rengine.ecs.Component;
import com.farias.rengine.ecs.ECSGame;
import com.farias.rengine.ecs.GameObject;
import com.farias.rengine.ecs.Transform;
import com.farias.rengine.ecs.gfx.Sprite;
import com.farias.rengine.ecs.gfx.TileMap;
import com.farias.rengine.render.RenderSystem;
import com.farias.rengine.render.Renderable;

public class RenderSystemECS extends RenderSystem {
	
	private Camera camera;
	
	public RenderSystemECS(ECSGame game, Camera camera) {
		super(game);
		this.camera = camera;
	}
	
	@Override
	public void init() {
		GL.createCapabilities();
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_TEXTURE_2D);
		System.out.println("render system initialized");	
	}

	@Override
	public void update(float deltaTime) {
		//clears all the pixel colors to black you can use glClearColor to change the
		// clear color
		glClear(GL_COLOR_BUFFER_BIT);
		
		camera.onUpdate(deltaTime);
		
		//TODO Interface Renderable vai para o componente e criar uma interface visible para o game object 
		for (GameObject e : ((ECSGame)game).getEntities()) {
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
		
		//MatrixPool.clear();
	}
	
	public Camera getCamera() {
		return camera;
	}
}
