package com.farias.rengine;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

import java.util.ArrayList;
import java.util.List;

import com.farias.rengine.event.EventSystem;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Window;
import com.farias.rengine.render.RenderSystem;

/**
 * The representation of game state
 * @author rafarias
 *
 */
public class Game implements Runnable {
	private Window window; 
	long framesPerSec = 60;
	long msPerFrame = 1000 / framesPerSec;
	List<GameObject> entities = new ArrayList<>();
	RenderSystem renderSystem;
	InputSystem inputSystem;
	EventSystem eventSystem;
	List<System> systems = new ArrayList<System>();
	
	public Game(Window window) {
		this.window = window;
	}
	
	public void init() {
		//initialize resources
		window.bind();
		renderSystem.init();
		for (GameObject e : entities) {
			e.init();
		}
	}
	
	public void run() {
		init();
		
		//game loop
		long start = java.lang.System.currentTimeMillis();
		while (!window.shouldClose()) {
			if (inputSystem.isKeyPressed(GLFW_KEY_ESCAPE)) {
				break;
			}
			
			long current = java.lang.System.currentTimeMillis();
			long deltaTime = current - start;
			start = current;
			

			//update input
			inputSystem.update(deltaTime);
			
			//update other systems
			for (System s: systems) {
				s.update(deltaTime);
			}
			
			//update entities
			for (GameObject gameObject : entities) {
				gameObject.update(deltaTime);
			}
			
			//render
			renderSystem.update(deltaTime);
			window.swapBuffers();
			try {
				Thread.sleep(java.lang.System.currentTimeMillis() - start + msPerFrame);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		glfwTerminate();
	}
	
	public void addEntity(GameObject entity) {
		this.entities.add(entity);
	}

	public void addSystem(System system) {
		if (system instanceof RenderSystem) {
			this.renderSystem = (RenderSystem) system;
		} else if (system instanceof InputSystem) {
			this.inputSystem = (InputSystem) system;
		} else if (system instanceof EventSystem) {
			this.eventSystem = (EventSystem) system;
		} else {
			this.systems.add(system);
		}
	}
	
	public List<GameObject> getEntities() {
		return entities;
	}
	
	public InputSystem getInputSystem() {
		return inputSystem;
	}
	
	public EventSystem getEventSystem() {
		return eventSystem;
	}
	
	public Window getWindow() {
		return window;
	}

	public RenderSystem getRenderSystem() {
		return renderSystem;
	}
}
