package com.farias.rengine.ecs;

import java.util.ArrayList;
import java.util.List;

import com.farias.rengine.Game;
import com.farias.rengine.ecs.render.Camera;
import com.farias.rengine.ecs.render.RenderSystemECS;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Window;

/**
 * A specialization of a game with Entity Component System (ECS) capabilities.
 * @author rafarias
 *
 */
public abstract class ECSGame extends Game {
	List<GameObject> entities = new ArrayList<>();

	public ECSGame(String title, Window window) {
		super(title, window);
	}

	public ECSGame(String title, int windowWidth, int windowHeight, Camera camera) {
		super(title, new Window(windowWidth, windowHeight));
		getWindow().setFullscreen(false);
		long windowId = getWindow().create();
		this.addSystem(new RenderSystemECS(camera));
		this.addSystem(new InputSystem(windowId));
	}
	
	public void onUserCreate() {
		for (GameObject e : entities) {
			e.init();
		}
	}
	
	public void onUserUpdate(float deltaTime) {
		for (GameObject gameObject : entities) {
			gameObject.update(deltaTime);
		}
	}
	
	public void addEntity(GameObject entity) {
		this.entities.add(entity);
		entity.init();
	}
	
	public List<GameObject> getEntities() {
		return entities;
	}
}
