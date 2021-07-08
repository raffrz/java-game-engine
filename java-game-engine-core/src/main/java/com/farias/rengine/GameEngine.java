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

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Manages a game instance and provides useful methods
 * that can be invoked from any part of the system
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

	//WINDOW
	public static int getWindowWidth() {
		return getGameInstance().getWindow().getWidth();
	}

	public static int getWindowHeight() {
		return getGameInstance().getWindow().getHeight();
	}

	//CAMERA
	static class Camera {
        Vector3f position = new Vector3f(0, 0, 0);
        Vector3f scale = new Vector3f(1, 1, 1);
        Vector3f rotation = new Vector3f();
        public Camera(){}
    }

	private static final Camera camera = new Camera();

	public static void setCamera(int x, int y) {
        camera.position.x = x;
        camera.position.y = y;
		setViewMatrix();
    }

	public static void rotateCamera(float x, float y) {
		camera.rotation.x = x;
		camera.rotation.y = y;
		setViewMatrix();
	}

    public static void setCameraZoom(float zoom) {
        camera.scale.set(zoom, zoom, 1);
		setViewMatrix();
    }

	// PROJECTIONS

	private static final Matrix4f projection_matrix = new Matrix4f();
	private static final Matrix4f view_matrix = new Matrix4f();
	private static final Matrix4f model_view_matrix = new Matrix4f();

	/**
	 * Sets the current projection to a simple orthographic projection.
	 * Works well with 2D Games
	 * @param width
	 * @param height
	 */
	public static void orthographicMode(int width, int height) {
		// projecao alternativa com o centro da tela sendo (width/2, height/2) o início da tela em (0, 0)
		// e final da tela em (width, -height)
		projection_matrix.setOrtho(0, width, -height, 0, -width, width);
			// .rotateX((float) Math.toRadians(-45f))
			// .rotateZ((float) Math.toRadians(-30f));

		// projecao padrao com o centro da tela sendo (0, 0) o início da tela em (-width/2, height/2)
		// e final da tela em (width/2, -height/2)
		//projection_matrix.setOrtho(-width/2, width/2, -height/2, height/2, -width, width);

		setViewMatrix();
	}

	private static Matrix4f setViewMatrix() {
		return view_matrix.identity()
			.rotate((float)Math.toRadians(camera.rotation.x), new Vector3f(1, 0, 0))
			.rotate((float)Math.toRadians(camera.rotation.y), new Vector3f(0, 1, 0))
			.translate(-camera.position.x, -camera.position.y, -camera.position.z);
	}

	private static Matrix4f getModelViewMatrix(Vector3f position, Vector3f rotation, Vector3f scale) {
		model_view_matrix.identity()
			.translate(position)
			.rotateX((float)Math.toRadians(-rotation.x))
			.rotateY((float)Math.toRadians(-rotation.y))
			.rotateZ((float)Math.toRadians(-rotation.z))
			.scale(scale);
		Matrix4f viewCurr = new Matrix4f(view_matrix);
		return viewCurr.mul(model_view_matrix);
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
		-1f, 1f, 0,   //TOP LEFT      0
		1f, 1f, 0,    //TOP RIGHT     1
		1f, -1f, 0,   //BOTTOM RIGHT  2
		-1f, -1f, 0,  //BOTTOM LEFT   3
	};
	
	private static int[] indices = new int[] {
		0,1,2,
		2,3,0
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

    public static void drawSprite(Sprite sprite, int x, int y, int width, int height) {
        Matrix4f modelViewMatrix = getModelViewMatrix(new Vector3f(x + width / 2, y - height /2, 0), 
			new Vector3f(0, 0, 0), new Vector3f(width/2, height/2, 1));

		Matrix4f projection = projection_matrix.mul(modelViewMatrix, model_view_matrix);

        drawSprite(sprite, projection);
    }

	public static void drawSprite(Sprite sprite, int tile, int tile_width, int tile_height, int x, int y, int width, int height) {
		sprite.model.setTexCoords(createTexCoords(sprite.texture, tile, tile_width, tile_height));
		drawSprite(sprite, x, y, width, height);
	}

	//TEXT
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
		Files.copy(
			initialStream, 
			targetFile.toPath(), 
			StandardCopyOption.REPLACE_EXISTING);
		initialStream.close();
	} 

	public static void drawText(String text, int x, int y, int width, int height) {
        int offset = - text.length() * width/2;
        for (char c: text.toCharArray()) {
            drawChar(c, x + offset, y, width, height);
            offset+=width;
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
				Matrix4f modelViewMatrix = getModelViewMatrix(new Vector3f(x + width * col + width /2, y + height * -row -height/2, 0), 
				new Vector3f(0, 0, 0), new Vector3f(width/2, -height/2, 1));
	
				Matrix4f projection = projection_matrix.mul(modelViewMatrix, model_view_matrix);
				map.shader.setUniform("projection", projection);
				map.model.draw();
			}
		}
	}

    // TEXTURES
	// TODO Rever função com image 256 x 64 e tile 32 x 32
    private static float[] createTexCoords(Texture t, int tile, float width, float height) {
		float hTiles = t.getWidth() / width;
		float vTiles = t.getHeight() / height;
		float sx = 1 / hTiles;
		float sy = 1 / vTiles;
		int tx = (int) (tile % hTiles);
		int ty = (int) (tile / vTiles);
		float[] texCoords = new float[] {
			sx * tx, sy * ty,
			sx * tx + sx, sy * ty,
			sx * tx + sx, sy * ty + sy,
			sx * tx, sy * ty + sy,
		};
		return texCoords;
	}
}
