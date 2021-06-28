package com.farias.rengine.examples;

import static org.lwjgl.glfw.GLFW.*;

import com.farias.rengine.Game;
import com.farias.rengine.GameEngine;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Window;
import com.farias.rengine.render.Model;
import com.farias.rengine.render.RenderSystem;
import com.farias.rengine.render.Shader;
import com.farias.rengine.render.Texture;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Simple2DGame extends Game {

    private static final int width = 640;
    private static final int height = 480;
    private static final float player_speed = 2.8f;
    private static final float camera_speed = 2.8f;
    private int cam_x = 0;
    private int cam_y = 0;
    private float cam_zoom = 1;
    private Sprite sprite;
    private Vector3f sprite_position;
    private Vector3f sprite_scale;

    public Simple2DGame(String title, Window window) {
        super(title, window);
    }

    @Override
    public void onUserCreate() {
        Engine.setOrthographicProjection(640, 480);
        sprite = Engine.createSprite("resources/character/Character_Down.png", 0, 32f, 32f);
        sprite_position = new Vector3f(0, 0, 0);
        sprite_scale = new Vector3f(32f, 32f, 1f);
    }

    @Override
    public void onUserUpdate(float deltaTime) {
        //CAMERA CONTROL
        if (getInputSystem().isKeyDown(GLFW_KEY_UP)) {
            cam_y -= 32 * deltaTime * camera_speed;
        }
        if (getInputSystem().isKeyDown(GLFW_KEY_DOWN)) {
            cam_y += 32 * deltaTime * camera_speed;
        }
        if (getInputSystem().isKeyDown(GLFW_KEY_LEFT)) {
            cam_x += 32 * deltaTime * camera_speed;
        }
        if (getInputSystem().isKeyDown(GLFW_KEY_RIGHT)) {
            cam_x -= 32 * deltaTime * camera_speed;
        }
        if (getInputSystem().isKeyDown(GLFW_KEY_Z)) {
            cam_zoom += 1 * deltaTime * camera_speed;
        }
        Engine.setCamera(cam_x, cam_y);
        Engine.setCameraZoom(cam_zoom);
        //SPRITE MOVEMENT
            if (getInputSystem().isKeyDown(GLFW_KEY_W)) {
            sprite_position.add(0, 32 * deltaTime * player_speed, 0);
        }
        if (getInputSystem().isKeyDown(GLFW_KEY_S)) {
            sprite_position.sub(0, 32 * deltaTime * player_speed, 0);
        }
        if (getInputSystem().isKeyDown(GLFW_KEY_A)) {
            sprite_position.sub(32 * deltaTime * player_speed, 0, 0);
        }
        if (getInputSystem().isKeyDown(GLFW_KEY_D)) {
            sprite_position.add(32 * deltaTime * player_speed, 0, 0);
        }
        //DEBUG
        if (getInputSystem().isKeyPressed(GLFW_KEY_F1)) {
            System.out.println("PLAYER_X: " + sprite_position.x);
            System.out.println("PLAYER_Y: " + sprite_position.y);
            System.out.println("CAM_X: " + cam_x);
            System.out.println("CAM_Y: " + cam_y);
        }
    }

    @Override
    public void onGfxUpdate(float deltaTime) {
        Engine.drawSprite(super.getWindow(), sprite, (int) sprite_position.x,
         (int) sprite_position.y, 32, 32);
    }

    public static void main(String[] args) {
        Window window = new Window(width, height);
        long windowId = window.create();
        Simple2DGame game = new Simple2DGame("Simple 2D Game", window);
        InputSystem input = new InputSystem(game, windowId);
        game.addSystem(input);
        game.addSystem(new RenderSystem(game) {});
        GameEngine.initGame(game);
    }

}

class Sprite {
    private Model model;
    private Texture texture;
    private Shader shader;

    public Sprite(Texture texture, Shader shader, Model model) {
        this.model = model;
        this.shader = shader;
        this.texture = texture;
    }

    public Model getModel() {
        return model;
    }

    public Shader getShader() {
        return shader;
    }

    public Texture getTexture() {
        return texture;
    }
}

class Engine {
    // CAMERA
    static class Camera {
        Vector3f position = new Vector3f(0, 0, 0);
        Vector3f scale = new Vector3f(1, 1, 1);
        Vector3f rotation = new Vector3f();
        public Camera(){}
    }

    public static void setCamera(int x, int y) {
        camera.position.x = x;
        camera.position.y = y;
    }

    public static void setCameraZoom(float zoom) {
        camera.scale.set(zoom, zoom, 1);
    }

    private static final Camera camera = new Camera();

    // PROJECTIONS
    private static final Matrix4f projection = new Matrix4f();

    public static void setOrthographicProjection(int width, int height) {
        // projecao alternativa com o centro da tela sendo (width/2, height/2) o início da tela em (0, 0)
        // e final da tela em (width, -height)
        projection.setOrtho(0, width, -height, 0, -width, width);
        
        // projecao padrao com o centro da tela sendo (0, 0) o início da tela em (-width/2, height/2)
        // e final da tela em (width/2, -height/2)
        //projection.setOrtho(-width/2, width/2, -height/2, height/2, -width, width);
    }

    public static Matrix4f getCameraProjection() {
        return getProjection()
            .translate(camera.position, new Matrix4f())
            .scale(camera.scale);
    }


    public static Matrix4f getProjection() {
        return projection;
    }

    // SPRITES

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
        sprite.getShader().bind();
		sprite.getTexture().bind(0);
		sprite.getShader().setUniform("sampler", 0);
        sprite.getShader().setUniform("projection", projection);
		sprite.getModel().draw();
    }

    public static void drawSprite(Window window, Sprite sprite, int x, int y, int width, int height) {    
        // sprite transformation
        Matrix4f transform = new Matrix4f()
            .setTranslation(x, y, 0)
            .scale(width, height, 1);

        Matrix4f projection = getCameraProjection().mul(transform, new Matrix4f());

        drawSprite(sprite, projection);
    }

    // TEXTURES

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