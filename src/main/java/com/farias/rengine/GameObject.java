package com.farias.rengine;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.farias.rengine.event.EventHandler;

public abstract class GameObject {
	private Map<Class<? extends Component>, Component> components = new HashMap<>();
	
	public GameObject() {
		
	}
	void update(long deltaTime) {
		for (Component c : components.values()) {
			c.update(deltaTime);
		}
	}
	public <T extends Component> void addComponent(T component) {
		this.components.put(component.getClass(), component);
		component.setGameObject(this);
	}
	@SuppressWarnings("unchecked")
	public <T extends EventHandler> T getComponent(Class<T> clazz) {
		return (T) components.get(clazz);
	}
	
	public Collection<Component> getComponents() {
		return components.values();
	}
}