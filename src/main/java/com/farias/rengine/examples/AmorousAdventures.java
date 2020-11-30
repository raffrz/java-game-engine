package com.farias.rengine.examples;

import org.joml.Vector3f;

import com.farias.rengine.Game;
import com.farias.rengine.GameEngine;
import com.farias.rengine.GameObject;
import com.farias.rengine.Transform;
import com.farias.rengine.gfx.Sprite;
import com.farias.rengine.gfx.TileMap;
import com.farias.rengine.render.Renderable;

public class AmorousAdventures extends Game {
	
	private static final float FLOOR_TILE_SIZE = 32f;
	
	private static final int MAP_SIZE = 32;

	static final String TITLE = "Amorous Adventures";
	
	static final int FLOOR_1 = 61;
	
	static final int screen_width = 640;
	static final int screen_height = 480;
	
	GameObject map;

	public AmorousAdventures() {
		super(TITLE, screen_width, screen_height);
	}
	
	@Override
	public void onUserCreate() {
		createMap();
		//createPlayer();
	}
	
	@Override
	public void onUserUpdate(float deltaTime) {
		
	}
	
	void createMap() {
		addEntity(new Map());
	}
	
	class Map extends GameObject implements Renderable {
		@Override
		public void onInit() {
			Transform transform = new Transform(0, 0);
			transform.setScale(new Vector3f(16f, 16f, 1));
			this.addComponent("transform", transform);
			
			Sprite s = new Sprite("resources/map/floor_tileset.png", FLOOR_1, FLOOR_TILE_SIZE, FLOOR_TILE_SIZE);
			TileMap m = new TileMap(s, MAP_SIZE, MAP_SIZE, FLOOR_TILE_SIZE);
			this.addComponent("tilemap", m);
		}
	}
	
	public static void main(String[] args) {
		Game game = new AmorousAdventures();
		GameEngine.initGame(game);
	}

}
