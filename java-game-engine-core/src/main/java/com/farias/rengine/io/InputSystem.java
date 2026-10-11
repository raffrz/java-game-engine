package com.farias.rengine.io;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.DoubleBuffer;

import org.lwjgl.BufferUtils;

public class InputSystem {
	
	public static final int KEY_FIRST = GLFW_KEY_SPACE;
	
	public static final int KEY_LAST = GLFW_KEY_LAST;

	private final Window window;
	private final long handle;
	
	private boolean keys[];

	private DoubleBuffer mouseX = BufferUtils.createDoubleBuffer(1);
	private DoubleBuffer mouseY = BufferUtils.createDoubleBuffer(1);
	
	public InputSystem(Window window) {
		this.window = window;
		this.handle = window.getHandle();
		this.keys = new boolean[KEY_LAST];
	}
	
	public boolean isKeyDown(int key) {
		return glfwGetKey(handle, key) == 1;
	}
	
	public boolean isKeyPressed(int key) {
		return isKeyDown(key) && !keys[key];
	}
	
	public boolean isKeyReleased(int key) {
		return !isKeyDown(key) && keys[key];
	}
	
	public boolean isMouseButtonDown(int button) {
		return glfwGetMouseButton(handle, button) == 1;
	}

	public double getMouseX() {
		return mouseX.get(0);
	}

	public double getMouseY() {
		return mouseY.get(0);
	}
	
	public void update() {
		//update input
		for (int i = KEY_FIRST; i < GLFW_KEY_LAST; i++)
			keys[i] = isKeyDown(i);
		
		glfwGetCursorPos(handle, mouseX, mouseY);
		
		window.pollEvents();
	}
}