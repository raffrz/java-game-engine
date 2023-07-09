package com.farias.rengine.io;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.DoubleBuffer;

import com.farias.rengine.Game;

import org.lwjgl.BufferUtils;

public class InputSystem implements com.farias.rengine.System {
	
	public static final int KEY_FIRST = GLFW_KEY_SPACE;
	
	public static final int KEY_LAST = GLFW_KEY_LAST;

	private long window;
	
	private boolean keys[];

	private DoubleBuffer mouseX = BufferUtils.createDoubleBuffer(1);
	private DoubleBuffer mouseY = BufferUtils.createDoubleBuffer(1);
	
	public InputSystem(long window) {
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

	public double getMouseX() {
		return mouseX.get(0);
	}

	public double getMouseY() {
		return mouseY.get(0);
	}
	
	@Override
	public void update(Game game, float deltaTime) {
		//update input
		for (int i = KEY_FIRST; i < GLFW_KEY_LAST; i++)
			keys[i] = isKeyDown(i);
		
		glfwGetCursorPos(window, mouseX, mouseY);
		
		glfwPollEvents();
	}
}