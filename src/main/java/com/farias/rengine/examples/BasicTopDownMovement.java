package com.farias.rengine.examples;

import com.farias.rengine.Game;
import com.farias.rengine.GameEngine;
import com.farias.rengine.GameObject;
import com.farias.rengine.Transform;
import com.farias.rengine.event.EventSystem;
import com.farias.rengine.gfx.Sprite;
import com.farias.rengine.gfx.TileSet;
import com.farias.rengine.gfx.Velocity;
import com.farias.rengine.io.Controllable;
import com.farias.rengine.io.Controller;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Window;
import com.farias.rengine.render.RenderSystem;
import com.farias.rengine.render.Renderable;
import com.farias.rengine.render.Texture;

public class BasicTopDownMovement {
	
	public static void main(String[] args) {
		Window window = new Window(800, 600);
		window.setFullscreen(false);
		long windowId = window.create("Destiny Warriors");
		
		//TODO create initialization methods for entities and components and remove this beforeLoop method
		Game game = new Game(window) {
			@Override
			public void beforeLoop() {
				this.addEntity(new Player());
				this.addEntity(new NPC());
			}
		};
		
		game.addSystem(new RenderSystem(game));
		game.addSystem(new InputSystem(game, windowId));
		game.addSystem(new EventSystem(game));
		
		GameEngine.initGame(game);
	}
	
}

class GameMap extends GameObject implements Renderable {
	TileSet tileSet;
	
	public GameMap() {
		this.addComponent(TileSet.load("assets/floor_tileset.gif", 32, 32));
	}
}

class Player extends GameObject implements Renderable, Controllable {
	Transform transform;
	TileSet tileSet;
	Sprite sprite;
	Controller controller;
	//Animation animation;
	//Physics physics;
	
	public Player() {
		this.addComponent(new Sprite(
				new Texture("resources/character/Character_Right.png"),
				1,
				32f, 32f));
		this.addComponent(new Transform(0, 0));
		this.addComponent(new Velocity());
		this.addComponent(new Controller());
	}
}

class NPC extends GameObject implements Renderable {
	Transform transform;
	TileSet tileSet;
	Sprite sprite;
	//Animation animation;
	//Physics physics;
	
	public NPC() {
		this.addComponent(new Transform(10, 10));
		this.addComponent(new Velocity());
		Sprite sprite = new Sprite(new Texture("resources/character/Character_Up.png"),
				1,
				32f, 32f);
		this.addComponent(sprite);
	}
}
