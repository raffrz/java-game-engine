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

public class BasicTopDownMovement extends Game {
	
	Player player;
	
	World world;
	
	static InputSystem input;
	
	public BasicTopDownMovement(Window window) {
		super("Destiny Warriors", window);
	}

	static int width = 640;
	static int height = 480;
	
	public static void main(String[] args) {
		Window window = new Window(width, height);
		window.setFullscreen(false);
		long windowId = window.create();
		
		//TODO create initialization methods for entities and components and remove this beforeLoop method
		Game game = new BasicTopDownMovement(window);
		game.addSystem(new RenderSystem(game, new TopDownCamera2D(width, height, 64f)));
		input = new InputSystem(game, windowId);
		game.addSystem(input);
		game.addSystem(new EventSystem(game));

		GameEngine.initGame(game);
		
		
	}
	
	@Override
	public void onUserCreate() {
		world = new World();
		player = new Player();
		
		this.addEntity(new World());
		this.addEntity(new NPC((float) (Math.random() * 20), (float) (Math.random() * 20)));
		this.addEntity(new Player());
	}

	@Override
	public void onUserUpdate(float deltaTime) {
		if (input.isKeyPressed(GLFW_KEY_SPACE)) {
			this.addEntity(new NPC((float) (Math.random() * 20.0f), (float) (Math.random() * 20.0f)));
		}
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
		Sprite stoneFloorSprite = new Sprite("resources/map/floor_tileset.png", 61, 32f, 32f);
		this.addComponent("tileMap", new TileMap(stoneFloorSprite, 32, 32, 32f));
	}

	@Override
	public void onUpdate(float deltaTime) {
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
	boolean standingUp = true;
	boolean standingDown;
	boolean standingLeft;
	boolean standingRight;
	final float speed = 1.3f;
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
		((TopDownCamera2D) GameEngine.getCamera()).setTarget(this);
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
	public void onUpdate(float deltaTime) {
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
	Vector3f spawnPosition;
	Transform transform;
	TileSet tileSet;
	Sprite walkingUp;
	Sprite walkingDown;
	Sprite walkingLeft;
	Sprite walkingRight;
	//Animation animation;
	//Physics physics;
	
	public NPC(float x, float y) {
		spawnPosition = new Vector3f(x, y, 0);
	}
	
	@Override
	public void onInit() {
		transform = new Transform(0, 0);
		transform.setScale(new Vector3f(32, 32, 1));
		transform.setPosition(spawnPosition);
		this.addComponent("transform", transform);
		this.addComponent("velocity", new Velocity());
		walkingUp = new Sprite("resources/character/Character_Up.png", 0, 32f, 32f);
		walkingDown = new Sprite("resources/character/Character_Down.png", 0, 32f, 32f);
		walkingLeft = new Sprite("resources/character/Character_Left.png", 0, 32f, 32f);
		walkingRight = new Sprite("resources/character/Character_Right.png", 0, 32f, 32f);

		this.addComponent("wakingDownSprite", walkingDown);
	}

	@Override
	public void onUpdate(float deltaTime) {
		// TODO Auto-generated method stub
		
	}
}
