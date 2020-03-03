package com.farias.rengine;

import com.farias.rengine.event.EventSystem;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.render.Camera;

/**
 * Manages a game instance and provides useful methods
 * that can be invoked from any part of the system
 * @author rafarias
 *
 */
public class GameEngine {
	private static Game game_instance;
	
	public static void initGame(Game game) {
		if (game_instance != null) {
			throw new IllegalStateException("Cannot create a more tha one game instance.");
		}
		game_instance = game;
		Thread t = new Thread(game_instance);
		t.start();
	}
	
	public static Game getGameInstance() {
		if (game_instance == null) {
			throw new IllegalStateException("Cannot obtain a game instance. The game must be created first.");
		}
		return game_instance;
	}
	
	public static InputSystem getInput() {
		return getGameInstance().getInputSystem();
	}
	
	public static EventSystem getEventSystem() {
		return getGameInstance().getEventSystem();
	}
	
	public static Camera getCamera() {
		return getGameInstance().getRenderSystem().getCamera();
	}	

	public static int getWindowWidth() {
		return getGameInstance().getWindow().getWidth();
	}

	public static int getWindowHeight() {
		return getGameInstance().getWindow().getHeight();
	}
}
