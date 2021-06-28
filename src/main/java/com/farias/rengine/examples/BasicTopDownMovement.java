package com.farias.rengine.examples;

import static org.lwjgl.glfw.GLFW.*;

import org.joml.Vector3f;

import com.farias.rengine.GameEngine;
import com.farias.rengine.ecs.GameObject;
import com.farias.rengine.ecs.Transform;
import com.farias.rengine.ecs.Velocity;
import com.farias.rengine.ecs.event.EventSystem;
import com.farias.rengine.ecs.gfx.Sprite;
import com.farias.rengine.ecs.gfx.TileMap;
import com.farias.rengine.ecs.gfx.TileSet;
import com.farias.rengine.ecs.input.Controller;
import com.farias.rengine.ecs.render.Camera;
import com.farias.rengine.ecs.render.RenderSystemECS;
import com.farias.rengine.ecs.ECSGame;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Window;
import com.farias.rengine.render.Renderable;

public class BasicTopDownMovement extends ECSGame {
	
	static InputSystem input;
	Player player;
	World world;
	static TopDownCamera2D camera;
	
	public BasicTopDownMovement(Window window) {
		super("Destiny Warriors", window);
	}

	static int width = 640;
	static int height = 480;
	
	public static void main(String[] args) {
		Window window = new Window(width, height);
		window.setFullscreen(false);
		long windowId = window.create();
		
		ECSGame game = new BasicTopDownMovement(window);
		camera = new TopDownCamera2D(width, height, 64f);
		game.addSystem(new RenderSystemECS(game, camera));
		input = new InputSystem(game, windowId);
		game.addSystem(input);
		game.addSystem(new EventSystem(game));

		GameEngine.initGame(game);
	}
	
	@Override
	public void onUserCreate() {
		super.onUserCreate();
		world = new World();
		player = new Player(camera);
		
		this.addEntity(new World());
		this.addEntity(new NPC((float) (Math.random() * 20), (float) (Math.random() * 20)));
		this.addEntity(player);
	}

	@Override
	public void onUserUpdate(float deltaTime) {
		super.onUserUpdate(deltaTime);
		if (input.isKeyDown(GLFW_KEY_SPACE)) {
			this.addEntity(new NPC((float) (Math.random() * 20.0f), (float) (Math.random() * 20.0f)));
		}
	}
	
}

class World extends GameObject implements Renderable {
	
	public World() {
	}
	
	@Override
	public void onInit() {
		System.out.println("creating world");
		this.addComponent("velocity", new Velocity());
		Transform transform = new Transform(0, 0);
		transform.setScale(new Vector3f(16, 16, 1));
//		transform.setRotation(new Vector3f((float) Math.toRadians(-45f), (float) Math.toRadians(0), (float) Math.toRadians(-30f)));
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

class Player extends GameObject implements Renderable {
	Camera camera;
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

	Player(Camera camera) {
		this.camera = camera;
	}
	
	@Override
	public void onInit() {
		System.out.println("creating player");
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
		((TopDownCamera2D) camera).setTarget(this);
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
	//Criar uma f�brica de sprites ou de resources tratar da questao de otimizacao de disco
	static final Sprite walkingUp = new Sprite("resources/character/Character_Up.png", 0, 32f, 32f);
	static final Sprite walkingDown = new Sprite("resources/character/Character_Down.png", 0, 32f, 32f);
	static final Sprite walkingLeft = new Sprite("resources/character/Character_Left.png", 0, 32f, 32f);
	static final Sprite walkingRight = new Sprite("resources/character/Character_Right.png", 0, 32f, 32f);
	
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

		this.addComponent("wakingDownSprite", walkingDown);
	}

	@Override
	public void onUpdate(float deltaTime) {
		// TODO Auto-generated method stub
		
	}
}
