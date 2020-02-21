package com.farias.rengine;

public abstract class System {
	protected Game game;
	
	public System(Game g) {
		this.game = g;
	}
	
	public void update(long deltaTime) {
		
	}
}
