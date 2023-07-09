package com.farias.rengine.ecs.event;

import java.util.HashSet;
import java.util.Set;

import com.farias.rengine.Game;
import com.farias.rengine.ecs.ECSGame;
import com.farias.rengine.ecs.GameObject;

public class EventSystem implements com.farias.rengine.System {
	
	private Set<String> events = new HashSet<>();
    private Game game;

    public EventSystem(Game game) {
        this.game = game;
    }
	
	public void triggerEvent(String event) {
		if (!events.contains(event)) {
			return;
		}
		for (GameObject e : ((ECSGame) game).getEntities()) {
			e.handleEvent(event);
			for (EventHandler c : e.getComponents()) {
				c.handleEvent(event);
			}
		}
	}

	public boolean isRegistered(String event) {
		return this.events.contains(event);
	}

	public void registerEvent(String event) {
		this.events.add(event);
	}

    @Override
    public void update(Game game, float deltaTime) {
        //not implemented
    }
	
}
