package com.farias.rengine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.farias.rengine.ecs.event.EventSystem;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.render.Model;
import com.farias.rengine.render.Shader;
import com.farias.rengine.render.Texture;
import com.farias.rengine.render.TexturedModel;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Manages a game instance and provides useful methods that can be invoked from
 * any part of the system
 * 
 * @author rafarias
 *
 */
public class GameEngine {
	private static Game game_instance;

	public static void initGame(Game game) {
		if (game_instance != null) {
			throw new IllegalStateException("Cannot create a more tha one game instance.");
		}
		game_instance = game;
		game_instance.run();
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

	// WINDOW
	public static int getWindowWidth() {
		return getGameInstance().getWindow().getWidth();
	}

	public static int getWindowHeight() {
		return getGameInstance().getWindow().getHeight();
	}

	// CAMERA
	static class Camera {
		Vector3f position = new Vector3f(0, 0, 0);
		Vector3f scale = new Vector3f(1, 1, 1);
		Vector3f rotation = new Vector3f();

		public Camera() {
		}

		public Matrix4f getViewMatrix() {
			return new Matrix4f().identity()
				.rotate((float) Math.toRadians(this.rotation.x), new Vector3f(1, 0, 0))
				.rotate((float) Math.toRadians(this.rotation.y), new Vector3f(0, 1, 0))
				.translate(-this.position.x, -this.position.y, -this.position.z);
		}
	}

	private static final Camera camera = new Camera();

	public static void setCamera(int x, int y, int z) {
		camera.position.x = x;
		camera.position.y = y;
		camera.position.z = z;
	}

	public static void setCamera(int x, int y) {
		camera.position.x = x;
		camera.position.y = y;
	}

	public static void rotateCamera(float x, float y) {
		camera.rotation.x = x;
		camera.rotation.y = y;
	}

	public static void setCameraZoom(float zoom) {
		camera.scale.x = zoom;
		camera.scale.y = zoom;
	}

	// PROJECTIONS

	private static final Matrix4f projection_matrix = new Matrix4f();

	/**
	 * Sets the current projection to a simple orthographic projection. Works well
	 * with 2D Games
	 * 
	 * @param width
	 * @param height
	 */
	public static void orthographicMode(int width, int height) {
		// projecao alternativa com o centro da tela sendo (width/2, height/2) o início
		// da tela em (0, 0)
		// e final da tela em (width, -height)
		projection_matrix.setOrtho(0, width, -height, 0, -width, width);
		// .rotateX((float) Math.toRadians(-45f))
		// .rotateZ((float) Math.toRadians(-30f));

		// projecao padrao com o centro da tela sendo (0, 0) o início da tela em
		// (-width/2, height/2)
		// e final da tela em (width/2, -height/2)
		// projection_matrix.setOrtho(-width/2, width/2, -height/2, height/2, -width,
		// width);
	}

	private static final int FOV = 70;
    private static final float Z_FAR = 1000f;
    private static final float Z_NEAR = 1.0f;

	public static void perspectiveMode(int width, int height) {
		float aspectRatio = (float)width / (float)height;
        projection_matrix.setPerspective(FOV, aspectRatio, Z_NEAR, Z_FAR);
	}

	private static Matrix4f getModelViewMatrix(Vector3f position, Vector3f rotation, Vector3f scale) {
		Matrix4f modelMatrix = getModelMatrix(position, rotation, scale);
		return camera.getViewMatrix().mul(modelMatrix, new Matrix4f());
	}

	private static Matrix4f getModelMatrix(Vector3f position, Vector3f rotation, Vector3f scale) {
		Matrix4f translationMatrix = new Matrix4f().translation(position);
		Matrix4f rotationMatrix = new Matrix4f().rotationXYZ((float) Math.toRadians(-rotation.x), (float) Math.toRadians(-rotation.y), (float) Math.toRadians(-rotation.z));
		Matrix4f scalingMatrix = new Matrix4f().scale(scale);

		return translationMatrix.mul(scalingMatrix.mul(rotationMatrix));
	}

	// SPRITES

	public static class Sprite {
		private Model model;
		private Texture texture;
		private Shader shader;

		public Sprite(Texture texture, Shader shader, Model model) {
			this.model = model;
			this.shader = shader;
			this.texture = texture;
		}
	}

	private static float[] vertices = new float[] { 
		-1f, 1f, 0, // TOP LEFT 0
		1f, 1f, 0, // TOP RIGHT 1
		1f, -1f, 0, // BOTTOM RIGHT 2
		-1f, -1f, 0, // BOTTOM LEFT 3
	};

	private static int[] indices = new int[] { 
		0, 1, 2,
		2, 3, 0 
	};

	public static Sprite createSprite(String file, int tile, float width, float height) {
		Texture texture = new Texture(file);
		Shader shader = new Shader("shader");
		float[] texCoords = createTexCoords(texture, tile, width, height);
		Model model = new Model(vertices, texCoords, indices);

		return new Sprite(texture, shader, model);
	}

	public static void drawSprite(Sprite sprite, Matrix4f projection) {
		sprite.shader.bind();
		sprite.texture.bind(0);
		sprite.shader.setUniform("sampler", 0);
		sprite.shader.setUniform("projection", projection);
		sprite.model.draw();
	}

	public static void drawSprite(Sprite sprite, int x, int y, int width, int height, float rotation) {
		Matrix4f modelViewMatrix = getModelViewMatrix(new Vector3f(x + width / 2, y - height / 2, 0),
				new Vector3f(0, 0, rotation), new Vector3f(width / 2, height / 2, 1));

		Matrix4f projection = projection_matrix.mul(modelViewMatrix, new Matrix4f());

		drawSprite(sprite, projection);
	}

	public static void drawSprite(Sprite sprite, int x, int y, int width, int height) {
		drawSprite(sprite, x, y, width, height, 0f);
	}

	public static void drawSprite(Sprite sprite, int tile, int tile_width, int tile_height, int x, int y, int width,
			int height) {
		drawSprite(sprite, tile, tile_width, tile_height, x, y, width, height, 0f);
	}

	public static void drawSprite(Sprite sprite, int tile, int tile_width, int tile_height, int x, int y, int width,
			int height, float rotation) {
		sprite.model.setTexCoords(createTexCoords(sprite.texture, tile, tile_width, tile_height));
		drawSprite(sprite, x, y, width, height, rotation);
	}

	// TEXT
	private static Sprite font;
	private static final String FONTS_FILE_NAME = "default16x16.png";

	private static Sprite getFont() {
		if (font == null) {
			try {
				createFontsFile();
				font = createSprite(FONTS_FILE_NAME, 0, 16, 16);
			} catch (IOException e) {
				java.lang.System.err.println("falha ao gravar o arquivo de fontes");
				java.lang.System.exit(0);
			}
		}
		return font;
	}

	private static void createFontsFile() throws IOException {
		InputStream initialStream = GameEngine.class.getClassLoader().getResourceAsStream("fonts/" + FONTS_FILE_NAME);
		File targetFile = new File(FONTS_FILE_NAME);
		Files.copy(initialStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		initialStream.close();
	}

	public static void drawText(String text, int x, int y, int width, int height) {
		int offset = -text.length() * width / 2;
		for (char c : text.toCharArray()) {
			drawChar(c, x + offset, y, width, height);
			offset += width;
		}
	}

	public static void drawChar(int c, int x, int y, int width, int height) {
		drawSprite(getFont(), c, 16, 16, x, y, width, height);
	}

	// MAP
	public static class Map {
		private Model model;
		private Texture texture;
		private Shader shader;

		public Map(Texture texture, Shader shader, Model model) {
			this.model = model;
			this.shader = shader;
			this.texture = texture;
		}
	}

	public static Map createMap(String file, int tile, float width, float height) {
		Texture texture = new Texture(file);
		Shader shader = new Shader("shader");
		float[] texCoords = createTexCoords(texture, tile, width, height);
		Model model = new Model(vertices, texCoords, indices);

		return new Map(texture, shader, model);
	}

	public static void drawMap(Map map, int x, int y, int width, int height, int celx, int cely, int celw, int celh) {
		map.shader.bind();
		map.texture.bind(0);
		map.shader.setUniform("sampler", 0);
		for (int row = 0; row < celh; row++) {
			for (int col = 0; col < celw; col++) {
				Matrix4f modelViewMatrix = getModelViewMatrix(
						new Vector3f(x + width * col + width / 2, y + height * -row - height / 2, 0),
						new Vector3f(0, 0, 0), new Vector3f(width / 2, -height / 2, 1));

				Matrix4f projection = projection_matrix.mul(modelViewMatrix, new Matrix4f());
				map.shader.setUniform("projection", projection);
				map.model.draw();
			}
		}
	}

	// TEXTURES
	private static float[] createTexCoords(Texture t, int tile, float width, float height) {
		float hTiles = t.getWidth() / width;
		float vTiles = t.getHeight() / height;
		float sx = 1 / hTiles;
		float sy = 1 / vTiles;
		int tx = (int) (tile % hTiles);
		int ty = (int) (tile / hTiles);
		float[] texCoords = new float[] { 
			sx * tx, sy * ty,
			sx * tx + sx, sy * ty,
			sx * tx + sx, sy * ty + sy,
			sx * tx, sy * ty + sy 
		};
		return texCoords;
	}

	//3D
	public static void drawTexturedModel(TexturedModel texturedModel) {
		Matrix4f modelMatrix = texturedModel.getModelMatrix();
		Matrix4f modelViewMatrix = camera.getViewMatrix().mul(modelMatrix, new Matrix4f());
        Matrix4f projection = projection_matrix.mul(modelViewMatrix, new Matrix4f());

		texturedModel.draw(projection);
	}
}
