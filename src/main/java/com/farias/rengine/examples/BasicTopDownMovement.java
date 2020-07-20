package com.farias.rengine.examples;

import static org.lwjgl.glfw.GLFW.*;

import org.joml.Vector3f;

import com.farias.rengine.Game;
import com.farias.rengine.GameEngine;
import com.farias.rengine.GameObject;
import com.farias.rengine.Transform;
import com.farias.rengine.event.EventSystem;
import com.farias.rengine.gfx.Sprite;
import com.farias.rengine.gfx.TileMap;
import com.farias.rengine.gfx.TileSet;
import com.farias.rengine.gfx.Velocity;
import com.farias.rengine.io.Controllable;
import com.farias.rengine.io.Controller;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Window;
import com.farias.rengine.render.RenderSystem;
import com.farias.rengine.render.Renderable;

public class BasicTopDownMovement {
	
	public static void main(String[] args) {
		Window window = new Window(640, 480);
		window.setFullscreen(false);
		long windowId = window.create("Destiny Warriors");
		
		//TODO create initialization methods for entities and components and remove this beforeLoop method
		Game game = new Game(window);
		
		game.addSystem(new RenderSystem(game));
		game.addSystem(new InputSystem(game, windowId));
		game.addSystem(new EventSystem(game));
		
		game.addEntity(new World());
		game.addEntity(new Player());
		game.addEntity(new NPC());
		
		GameEngine.initGame(game);
	}
	
}

class World extends GameObject implements Renderable {
	
	public World() {
	}
	
	@Override
	public void onInit() {
		this.addComponent("velocity", new Velocity());
		Transform transform = new Transform(0, 0);
		transform.setScale(new Vector3f(16, 16, 1));
		this.addComponent("transform", transform);
		Sprite stoneFloorSprite = new Sprite("resources/map/floor_tileset.png", 64, 32f, 32f);
		this.addComponent("tileMap", new TileMap(stoneFloorSprite, 24, 24, 32f));
	}

	@Override
	public void onUpdate(long deltaTime) {
		// TODO Auto-generated method stub
	}
}

enum CreatureState {
	WALKING_UP, WALKING_DOWN, WALKING_LEFT, WALKING_RIGHT, 
	STANDING_UP, STANDING_DOWN, STANDING_LEFT, STANDING_RIGHT
}

class Player extends GameObject implements Renderable, Controllable {
	Transform transform;
	Velocity velocity;
	Controller controller;
	Sprite sprWalkingUp;
	Sprite sprWalkingDown;
	Sprite sprWalkingLeft;
	Sprite sprWalkingRight;
	boolean walkingUp;
	boolean walkingDown;
	boolean walkingLeft;
	boolean walkingRight;
	boolean standingUp;
	boolean standingDown = true;
	boolean standingLeft;
	boolean standingRight;
	final float speed = 2.6f;
	//Animation animation;
	//Physics physics;
	
	@Override
	public void onInit() {
		this.velocity = new Velocity();
		this.addComponent("velocity", velocity);
		
		transform = new Transform(0, 0);
		transform.setScale(new Vector3f(32, 32, 1));
		
		this.addComponent("transform", transform);
		
		sprWalkingUp = new Sprite("resources/character/Character_Up.png", 0, 32f, 32f);
		sprWalkingDown = new Sprite("resources/character/Character_Down.png", 0, 32f, 32f);
		sprWalkingLeft = new Sprite("resources/character/Character_Left.png", 0, 32f, 32f);
		sprWalkingRight = new Sprite("resources/character/Character_Right.png", 0, 32f, 32f);
		this.addComponent("wakingUpSprite", sprWalkingUp);
		this.addComponent("wakingDownSprite", sprWalkingDown);
		this.addComponent("wakingLeftSprite", sprWalkingLeft);
		this.addComponent("wakingRightSprite", sprWalkingRight);
		
		Controller controller = new Controller();
		controller.addButton(GLFW_KEY_W, "up");
		controller.addButton(GLFW_KEY_S, "down");
		controller.addButton(GLFW_KEY_A, "left");
		controller.addButton(GLFW_KEY_D, "right");
		this.addComponent("controller", controller);
		GameEngine.getCamera().setTarget(this);
	}
	
	@Override
	public void handleEvent(String event) {
		if (event.equals("up_pressed")) {
			walkingUp = true;
			standingUp = false;
			standingDown = false;
			standingLeft = false;
			standingRight = false;
		}
		if (event.equals("down_pressed")) {
			walkingDown = true;
			standingUp = false;
			standingDown = false;
			standingLeft = false;
			standingRight = false;
		} 
		if (event.equals("left_pressed")) {
			walkingLeft = true;
			standingUp = false;
			standingDown = false;
			standingLeft = false;
			standingRight = false;
		}
		if (event.equals("right_pressed")) {
			walkingRight = true;
			standingUp = false;
			standingDown = false;
			standingLeft = false;
			standingRight = false;
		}
		if (event.equals("up_released")) {
			walkingUp = false;
			standingUp = true;
		}
		if (event.equals("down_released")) {
			walkingDown = false;
			standingDown = true;
		}
		if (event.equals("left_released")) {
			walkingLeft = false;
			standingLeft = true;
		}
		if (event.equals("right_released")) {
			walkingRight = false;
			standingRight = true;
		}
	}
	
	@Override
	public void onUpdate(long deltaTime) {
		sprWalkingUp.hide();
		sprWalkingDown.hide();
		sprWalkingLeft.hide();
		sprWalkingRight.hide();
		velocity.setVy(0);
		velocity.setVx(0);
		if (walkingUp || standingUp) {
			if (walkingUp)
				velocity.setVy(speed);
			sprWalkingUp.show();
		} else if (walkingDown || standingDown) {
			if (walkingDown)
				velocity.setVy(-speed);
			sprWalkingDown.show();
		} else if (walkingLeft || standingLeft) {
			if (walkingLeft)
				velocity.setVx(-speed);
			sprWalkingLeft.show();
		} else if (walkingRight || standingRight) {
			if (walkingRight)
				velocity.setVx(speed);
			sprWalkingRight.show();
		}
	}
}

class NPC extends GameObject implements Renderable {
	Transform transform;
	TileSet tileSet;
	Sprite walkingUp;
	Sprite walkingDown;
	Sprite walkingLeft;
	Sprite walkingRight;
	//Animation animation;
	//Physics physics;
	
	@Override
	public void onInit() {
		transform = new Transform(0, 0);
		transform.setScale(new Vector3f(32, 32, 1));
		transform.setPosition(new Vector3f(4, 4, 0));
		this.addComponent("transform", transform);
		this.addComponent("velocity", new Velocity());
		walkingUp = new Sprite("resources/character/Character_Up.png", 0, 32f, 32f);
		walkingDown = new Sprite("resources/character/Character_Down.png", 0, 32f, 32f);
		walkingLeft = new Sprite("resources/character/Character_Left.png", 0, 32f, 32f);
		walkingRight = new Sprite("resources/character/Character_Right.png", 0, 32f, 32f);

		this.addComponent("wakingDownSprite", walkingDown);
	}

	@Override
	public void onUpdate(long deltaTime) {
		// TODO Auto-generated method stub
		
	}
}
