package com.farias.rengine;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import java.awt.Event;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {
	
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

/**
 * Manages a game instance and provides useful methods
 * that can be invoked from any part of the system
 * @author rafarias
 *
 */
class GameEngine {
	private static Game game_instance;
	
	public static void initGame(Game game) {
		if (game_instance != null) {
			throw new IllegalStateException("Cannot create a more tha one game instance.");
		}
		game_instance = game;
		Thread t = new Thread(game_instance);
		t.start();
	}
	
	public static Game getGameInstance() {
		if (game_instance == null) {
			throw new IllegalStateException("Cannot obtain a game instance. The game must be created first.");
		}
		return game_instance;
	}
	
	public static InputSystem getInput() {
		return getGameInstance().getInputSystem();
	}
	
	public static EventSystem getEventSystem() {
		return getGameInstance().getEventSystem();
	}

	public static int getWindowWidth() {
		return getGameInstance().getWindow().getWidth();
	}

	public static int getWindowHeight() {
		return getGameInstance().getWindow().getHeight();
	}
}

/**
 * The representation of game state
 * @author rafarias
 *
 */
class Game implements Runnable {
	private Window window; 
	long framesPerSec = 60;
	long msPerFrame = 1000 / framesPerSec;
	List<GameObject> entities = new ArrayList<>();
	RenderSystem renderSystem;
	InputSystem inputSystem;
	EventSystem eventSystem;
	List<System> systems = new ArrayList<System>();
	
	Game(Window window) {
		this.window = window;
	}
	
	public void beforeLoop() {
		
	}
	
	public void run() {
		//initialize resources
		window.bind();
		renderSystem.init();
		this.beforeLoop();
		
		//game loop
		long start = java.lang.System.currentTimeMillis();
		while (!window.shouldClose()) {
			if (inputSystem.isKeyPressed(GLFW_KEY_ESCAPE)) {
				break;
			}
			
			long current = java.lang.System.currentTimeMillis();
			long deltaTime = current - start;
			start = current;
			

			//update input
			inputSystem.update(deltaTime);
			
			//update other systems
			for (System s: systems) {
				s.update(deltaTime);
			}
			
			//update entities
			for (GameObject gameObject : entities) {
				gameObject.update(deltaTime);
			}
			
			//render
			renderSystem.update(deltaTime);
			window.swapBuffers();
			try {
				Thread.sleep(java.lang.System.currentTimeMillis() - start + msPerFrame);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		glfwTerminate();
	}
	
	public void addEntity(GameObject entity) {
		this.entities.add(entity);
	}

	public void addSystem(System system) {
		if (system instanceof RenderSystem) {
			this.renderSystem = (RenderSystem) system;
		} else if (system instanceof InputSystem) {
			this.inputSystem = (InputSystem) system;
		} else if (system instanceof EventSystem) {
			this.eventSystem = (EventSystem) system;
		} else {
			this.systems.add(system);
		}
	}
	
	public InputSystem getInputSystem() {
		return inputSystem;
	}
	
	public EventSystem getEventSystem() {
		return eventSystem;
	}
	
	public Window getWindow() {
		return window;
	}
}

class Window {
	private long id;
	private int width;
	private int height;
	private boolean fullscreen;
	
	public Window(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public void bind() {
		glfwMakeContextCurrent(id);		
	}

	public long create(String title) {
		// init GLFW
		if (!glfwInit()) {
			throw new IllegalStateException("Failed to initialize GLFW");
		}
		
		// creating window
		this.id = glfwCreateWindow(width, height, title,
				fullscreen ? glfwGetPrimaryMonitor() : 0, 0);
		if (this.id == 0) {
			throw new IllegalStateException("Failed to create window");
		}
		
		if (!fullscreen) {
			//moves window to the center
			GLFWVidMode videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
			glfwSetWindowPos(id,
					(videoMode.width() - width) / 2,
					(videoMode.height() - height) / 2);
			
			glfwShowWindow(id);
		}
		
		return this.id;
	}
	
	public void setFullscreen(boolean fullscreen) {
		this.fullscreen = fullscreen;
	}
	
	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public boolean shouldClose() {
		return glfwWindowShouldClose(id);
	}

	public void swapBuffers() {
		glfwSwapBuffers(id);
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
}

//systems

abstract class System {
	Game game;
	
	public System(Game g) {
		this.game = g;
	}
	
	public void update(long deltaTime) {
		
	}
}

class RenderSystem extends System {
	
	//TODO Refactor
	private Camera camera;
	
	public RenderSystem(Game game) {
		super(game);
	}
	
	public void init() {
		camera = new Camera(GameEngine.getWindowWidth(), GameEngine.getWindowHeight());
		GL.createCapabilities();
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_TEXTURE_2D);		
	}

	public void update(long deltaTime) {
		//clears all the pixel colors to black you can use glClearColor to change the
		// clear color
		glClear(GL_COLOR_BUFFER_BIT);
		
		for (GameObject e : game.entities) {
			if (e instanceof Renderable) {
				Transform transform = e.getComponent(Transform.class);
				Sprite sprite = e.getComponent(Sprite.class);
				//render sprites
				if (transform != null && sprite != null) {
					this.render(transform, sprite);
				}
			}
		}
	}
	
	public void render(Transform transform, Sprite sprite) {
		int sampler = 0;
		sprite.draw(camera, transform, sampler);
	}
}

class InputSystem extends System {
	
	public static final int KEY_FIRST = GLFW_KEY_SPACE;
	
	public static final int KEY_LAST = GLFW_KEY_LAST;

	private long window;
	
	private boolean keys[];
	
	public InputSystem(Game game, long window) {
		super(game);
		this.window = window;
		this.keys = new boolean[KEY_LAST];
	}
	
	public boolean isKeyDown(int key) {
		return glfwGetKey(window, key) == 1;
	}
	
	public boolean isKeyPressed(int key) {
		return isKeyDown(key) && !keys[key];
	}
	
	public boolean isKeyReleased(int key) {
		return !isKeyDown(key) && keys[key];
	}
	
	public boolean isMouseButtonDown(int button) {
		return glfwGetMouseButton(window, button) == 1;
	}
	
	@Override
	public void update(long deltaTime) {
		//update input
		for (int i = KEY_FIRST; i < GLFW_KEY_LAST; i++)
			keys[i] = isKeyDown(i);
		
		glfwPollEvents();
	}
}

class EventSystem extends System {
	
	private Set<String> events = new HashSet<>();;

	public EventSystem(Game g) {
		super(g);
	}
	
	public void triggerEvent(String event) {
		if (!events.contains(event)) {
			return;
		}
		for (GameObject e : game.entities) {
			for (Component c : e.components.values()) {
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
	
}

//entities

abstract class GameObject {
	Map<Class<? extends Component>, Component> components = new HashMap<>();
	
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
	public <T extends Component> T getComponent(Class<T> clazz) {
		return (T) components.get(clazz);
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
		this.addComponent(new Sprite(
				new Texture("resources/character/Character_Up.png"),
				1,
				32f, 32f));
		this.addComponent(new Transform(10, 10));
		this.addComponent(new Velocity());
	}
}

//components

abstract class Component {
	private GameObject gameObject;
	public void update(long deltaTime) {
		
	}
	public void handleEvent(String event) {
		
	}
	public GameObject getGameObject() {
		return gameObject;
	}
	public void setGameObject(GameObject gameObject) {
		this.gameObject = gameObject;
	}
}

class Transform extends Component {
	Vector2f position;
	Vector2f rotation = new Vector2f();
	Matrix4f scale;
	
	public Transform(float x, float y) {
		this.position = new Vector2f(x / 100, y / 100);
		this.scale = new Matrix4f()
				.translate(new Vector3f(0, 0, 0))
				.scale(128);
	}
	
	@Override
	public void update(long deltaTime) {
		Velocity v = getGameObject().getComponent(Velocity.class);
		position.x += v.getVx() / 100;
		position.y += v.getVy() / 100;
	}
}

class Velocity extends Component {
	float vx;
	float vy;
	
	@Override
	public void handleEvent(String event) {
		if (!(getGameObject() instanceof Controllable)) {
			return;
		}
		if (event.contentEquals("up_pressed")) {
			vy = 1;
		}
		if (event.contentEquals("down_pressed")) {
			vy = -1;
		} 
		if (event.contentEquals("left_pressed")) {
			vx = -1;
		}
		if (event.contentEquals("right_pressed")) {
			vx = 1;
		}
		if (event.contentEquals("up_released") ||
				event.contentEquals("down_released")) {
			vy = 0;
		}
		if (event.contentEquals("left_released") || 
				event.contentEquals("right_released")) {
			vx = 0;
		}
	}
	
	@Override
	public void update(long deltaTime) {

	}
	
	public float getVx() {
		return vx;
	}
	
	public float getVy() {
		return vy;
	}
	
	public void setVx(float vx) {
		this.vx = vx;
	}
	
	public void setVy(float vy) {
		this.vy = vy;
	}
}

class TileSet extends Component {
	String path;
	Texture texture;
	int width;
	int height;
	
	public static TileSet load(String filename, int width, int heigth) {
		TileSet t = new TileSet();
		t.height = heigth;
		t.width = width;
		t.texture = new Texture(filename);
		return t;
	}
	
	public void bindTexture(int sampler) {
		this.texture.bind(sampler);
	}
}

class Sprite extends Component {
	Vector2f dimension;
	private Model model;
	private Texture texture;
	private Shader shader;
	int tile;
	boolean tileChanged;
	long passed;
	
	//how can i convert this float values to pixels width and height and vice versa?
	static float[] vertices = new float[] {
		-0.5f, 0.5f, 0,   //TOP LEFT      0
		0.5f, 0.5f, 0,    //TOP RIGHT     1
		0.5f, -0.5f, 0,   //BOTTOM RIGHT  2
		-0.5f, -0.5f, 0,  //BOTTOM LEFT   3
	};
	
	static int[] indices = new int[] {
		0,1,2,
		2,3,0
	};
	
	@Override
	public void update(long deltaTime) {
		//TODO Create an Animation component
		//animate sprites
		passed += deltaTime;
		if (passed >= 1000 / 60 * 20) {
			Velocity v = getGameObject().getComponent(Velocity.class);
			if (v.getVx() != 0) {
				tile++;
				model.setTexCoords(createTexCoords(tile));
			}
			passed = 0;
		}
	}
	
	public Sprite(Texture texture, int tile, float width, float height) {
		this.dimension = new Vector2f(width, height);
		this.tile = tile;
		this.shader = new Shader("shader");
		this.texture = texture;
		float[] texCoords = createTexCoords(tile);
		this.model = new Model(vertices, texCoords, indices);
	}

	public void draw(Camera camera, Transform transform, int sampler) {
		shader.bind();
		texture.bind(sampler);
		Matrix4f tile_pos = new Matrix4f().translate(new Vector3f(transform.position.x * 2, 
				transform.position.y * 2, 0));
		Matrix4f target = new Matrix4f();
		
		camera.getProjection().mul(transform.scale, target);
		target.mul(tile_pos);
		
		shader.setUniform("sampler", 0);
		shader.setUniform("projection", target);
		model.draw();
	}
	
	private float[] createTexCoords(int tile) {
		float hTiles = texture.getWidth() / dimension.x;
		float vTiles = texture.getHeight() / dimension.y;
		float sx = 1 / hTiles;
		float sy = 1 / vTiles;
		float[] texCoords = new float[] {
			sx * tile, sy * tile,
			sx * tile + sx, sy * tile,
			sx * tile + sx, sy * tile + sy,
			sx * tile, sy * tile + sy,
		};
		return texCoords;
	}

	public Vector2f getDimension() {
		return dimension;
	}
	
	public int getTile() {
		return tile;
	}
	
	public Model getModel() {
		return model;
	}
	
	public Texture getTexture() {
		return texture;
	}
	
	public Shader getShader() {
		return shader;
	}
}

class Controller extends Component {
	private String[] buttons;
	
	public Controller() {
		this.buttons = new String[InputSystem.KEY_LAST];
		this.addButton(GLFW_KEY_UP, "up");
		this.addButton(GLFW_KEY_DOWN, "down");
		this.addButton(GLFW_KEY_LEFT, "left");
		this.addButton(GLFW_KEY_RIGHT, "right");
	}
	
	@Override
	public void update(long deltaTime) {
		for (int i = 0; i < buttons.length; i++) {
			String button = buttons[i];
			if (GameEngine.getInput().isKeyPressed(i)) {
				String keyPressEvent = button + "_pressed";
				if (GameEngine.getEventSystem().isRegistered(keyPressEvent)) {
					GameEngine.getEventSystem().triggerEvent(keyPressEvent);
				}
			}
			if (GameEngine.getInput().isKeyReleased(i)) {
				String keyReleasedEvent = button + "_released";
				if (GameEngine.getEventSystem().isRegistered(keyReleasedEvent)) {
					GameEngine.getEventSystem().triggerEvent(keyReleasedEvent);
				}
			}
		}
	}
	
	public void addButton(int code, String button) {
		buttons[code] = button;
		String keyPressEvent = button + "_pressed";
		String keyReleasedEvent = button + "_released";
		GameEngine.getEventSystem().registerEvent(keyPressEvent);
		GameEngine.getEventSystem().registerEvent(keyReleasedEvent);
	}
}

//interfaces

interface Renderable {
}

interface Controllable {
	
}