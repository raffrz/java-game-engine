package com.farias.rengine;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

import java.util.ArrayList;
import java.util.List;

import com.farias.rengine.ecs.event.EventSystem;
import com.farias.rengine.render.RenderSystem;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Window;

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
	RenderSystem renderSystem;
	InputSystem inputSystem;
	EventSystem eventSystem;
	List<System> systems = new ArrayList<System>();
	
	public Game(String title, Window window) {
		this.title = title;
		this.window = window;
	}
	
	public void init() {
		//initialize resources
		window.bind();
		renderSystem.init();
		this.onUserCreate();
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
			
			float deltaTime = updateTime/1000f;
			//update game state
			if (updateTime > msPerFrame) {
				//update input system
				inputSystem.update(deltaTime);	
				//update other systems
				for (System s: systems) {
					s.update(deltaTime);
				}
				//update game 
				this.onUserUpdate(deltaTime);
				updateTime = 0;
			}
			//render game
			if (!frameLimiter || renderTime > msPerFrame) {
				renderSystem.update(deltaTime);
				this.onGfxUpdate(deltaTime);
				window.swapBuffers();
				frameCount++;
				renderTime = 0;
			}
		}
		glfwTerminate();
	}
	
	public abstract void onUserCreate();
	
	public abstract void onUserUpdate(float deltaTime);

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

	public void onGfxUpdate(float deltaTime) {

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
