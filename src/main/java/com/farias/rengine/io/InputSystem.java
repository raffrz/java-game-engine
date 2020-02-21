package com.farias.rengine.io;

import static org.lwjgl.glfw.GLFW.*;

import com.farias.rengine.Game;

public class InputSystem extends com.farias.rengine.System {
	
	public static final int KEY_FIRST = GLFW_KEY_SPACE;
	
	public static final int KEY_LAST = GLFW_KEY_LAST;

	private long window;
	
	private boolean keys[];
	
	public InputSystem(Game game, long window) {
		super(game);
		this.window = window;
		this.keys = new boolean[KEY_LAST];
	}
	
	public boolean isKeyDown(int key) {
		return glfwGetKey(window, key) == 1;
	}
	
	public boolean isKeyPressed(int key) {
		return isKeyDown(key) && !keys[key];
	}
	
	public boolean isKeyReleased(int key) {
		return !isKeyDown(key) && keys[key];
	}
	
	public boolean isMouseButtonDown(int button) {
		return glfwGetMouseButton(window, button) == 1;
	}
	
	@Override
	public void update(long deltaTime) {
		//update input
		for (int i = KEY_FIRST; i < GLFW_KEY_LAST; i++)
			keys[i] = isKeyDown(i);
		
		glfwPollEvents();
	}
}