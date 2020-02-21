package com.farias.rengine.io;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;

import com.farias.rengine.Component;
import com.farias.rengine.GameEngine;

public class Controller extends Component {
	private String[] buttons;
	
	public Controller() {
		this.buttons = new String[InputSystem.KEY_LAST];
		this.addButton(GLFW_KEY_UP, "up");
		this.addButton(GLFW_KEY_DOWN, "down");
		this.addButton(GLFW_KEY_LEFT, "left");
		this.addButton(GLFW_KEY_RIGHT, "right");
	}
	
	@Override
	public void update(long deltaTime) {
		for (int i = 0; i < buttons.length; i++) {
			String button = buttons[i];
			if (GameEngine.getInput().isKeyPressed(i)) {
				String keyPressEvent = button + "_pressed";
				if (GameEngine.getEventSystem().isRegistered(keyPressEvent)) {
					GameEngine.getEventSystem().triggerEvent(keyPressEvent);
				}
			}
			if (GameEngine.getInput().isKeyReleased(i)) {
				String keyReleasedEvent = button + "_released";
				if (GameEngine.getEventSystem().isRegistered(keyReleasedEvent)) {
					GameEngine.getEventSystem().triggerEvent(keyReleasedEvent);
				}
			}
		}
	}
	
	public void addButton(int code, String button) {
		buttons[code] = button;
		String keyPressEvent = button + "_pressed";
		String keyReleasedEvent = button + "_released";
		GameEngine.getEventSystem().registerEvent(keyPressEvent);
		GameEngine.getEventSystem().registerEvent(keyReleasedEvent);
	}
}
