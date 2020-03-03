package com.farias.rengine;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.farias.rengine.event.EventHandler;
import com.farias.rengine.gfx.Sprite;

public abstract class GameObject implements EventHandler {
	private Map<String, Component> components = new HashMap<>();
	
	public GameObject() {
		
	}
	
	public void init() {
		this.onInit();
		for (Component c : components.values()) {
			c.init();
		}
	}
	
	public void onInit() {
		
	}
	
	@Override
	public void handleEvent(String event) {
	}
	
	void update(long deltaTime) {
		this.onUpdate(deltaTime);
		for (Component c : components.values()) {
			c.update(deltaTime);
		}
	}
	
	public abstract void onUpdate(long deltaTime);
	
	public <T extends Component> void addComponent(String name, T component) {
		this.components.put(name, component);
		component.setGameObject(this);
	}
	public void removeComponent(String name) {
		getComponent(name).setGameObject(null);;
		this.components.remove(name);
	}
	public Component getComponent(String name) {
		return components.get(name);
	}
	
	public Collection<Component> getComponents() {
		return components.values();
	}
}