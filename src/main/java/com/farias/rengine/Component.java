package com.farias.rengine;

import com.farias.rengine.event.EventHandler;

public abstract class Component implements EventHandler {
	protected GameObject gameObject;
	public void init() {
		
	}
	public void update(long deltaTime) {
		
	}
	@Override
	public void handleEvent(String event) {
		
	}
	public GameObject getGameObject() {
		return gameObject;
	}
	public void setGameObject(GameObject gameObject) {
		this.gameObject = gameObject;
	}
}
