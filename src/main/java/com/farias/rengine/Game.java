package com.farias.rengine;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

import java.util.ArrayList;
import java.util.List;

import com.farias.rengine.event.EventSystem;
import com.farias.rengine.examples.BasicTopDownMovement;
import com.farias.rengine.examples.TopDownCamera2D;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Window;
import com.farias.rengine.render.RenderSystem;

/**
 * The representation of game state
 * @author rafarias
 *
 */
public abstract class Game {
	private String title;
	private Window window; 
	long framesPerSec = 60;
	long msPerFrame = 1000 / framesPerSec;
	boolean frameLimiter = false;
	List<GameObject> entities = new ArrayList<>();
	RenderSystem renderSystem;
	InputSystem inputSystem;
	EventSystem eventSystem;
	List<System> systems = new ArrayList<System>();
	
	public Game(String title, Window window) {
		this.title = title;
		this.window = window;
	}
	
	public Game(String title, int windowWidth, int windowHeight) {
		this.title = title;
		this.window = new Window(windowWidth, windowHeight);
		window.setFullscreen(false);
		long windowId = window.create();
		this.addSystem(new RenderSystem(this, new TopDownCamera2D(windowWidth, windowHeight, 0.0f)));
		this.addSystem(new InputSystem(this, windowId));
	}
	
	public void init() {
		//initialize resources
		window.bind();
		renderSystem.init();
		this.onUserCreate();
		for (GameObject e : entities) {
			e.init();
		}
	}
	
	public void run() {
		init();
		
		//game loop
		long start = java.lang.System.currentTimeMillis();
		long fpsTime = 0;
		long updateTime = 0;
		long renderTime = 0;
		int frameCount = 0;
		while (!window.shouldClose()) {
			if (inputSystem.isKeyPressed(GLFW_KEY_ESCAPE)) {
				break;
			}
			
			long current = java.lang.System.currentTimeMillis();
			long elapsed = current - start;
			start = current;
			fpsTime += elapsed;
			updateTime += elapsed;
			renderTime += elapsed;
			
			//Display FPS
			if (fpsTime > 1000) {
				window.setTitle(title + " - FPS: " + frameCount);
				frameCount = 0;
				fpsTime = 0;
			}
			
			//UPDATE GAME
			if (updateTime > msPerFrame) {
				float deltaTime = updateTime/1000f;
				
				//update input
				inputSystem.update(deltaTime);
				
				//update other systems
				for (System s: systems) {
					s.update(deltaTime);
				}
				
				this.onUserUpdate(deltaTime);
				
				//update entities
				for (GameObject gameObject : entities) {
					gameObject.update(deltaTime);
				}
				updateTime = 0;
			}
			
			//RENDER GAME
			if (!frameLimiter || renderTime > msPerFrame) {
				renderSystem.update(updateTime);
				window.swapBuffers();
				frameCount++;
				renderTime = 0;
			}
		}
		glfwTerminate();
	}
	
	public abstract void onUserCreate();
	
	public abstract void onUserUpdate(float deltaTime);
	
	public void addEntity(GameObject entity) {
		this.entities.add(entity);
		entity.init();
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
