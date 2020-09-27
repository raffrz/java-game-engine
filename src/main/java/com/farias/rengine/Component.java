package com.farias.rengine;

import com.farias.rengine.event.EventHandler;

public abstract class Component implements EventHandler {
	protected GameObject gameObject;
	void init() {
		this.onInit();
	}
	
	public void onInit() {
		
	}
	public void update(float deltaTime) {
		
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
