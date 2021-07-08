package com.farias.rengine.examples;

import static org.lwjgl.glfw.GLFW.*;
import static com.farias.rengine.GameEngine.*;

import com.farias.rengine.Game;
import com.farias.rengine.GameEngine;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Window;
import com.farias.rengine.render.RenderSystem;
import org.joml.Vector3f;

public class Simple2DGame extends Game {

    private static final int width = 1280;
    private static final int height = 960;
    private static final float player_speed = 2.8f;
    private static final float camera_speed = 2.8f;
    private int cam_x = 0;
    private int cam_y = 0;
    private Sprite sprite;
    private Map map;
    private Vector3f sprite_position;
    private Vector3f sprite_scale;

    public Simple2DGame(String title, Window window) {
        super(title, window);
    }

    @Override
    public void onUserCreate() {
        orthographicMode(640, 480);
        sprite = createSprite("resources/character/Character_Down.png", 0, 32f, 32f);
        map = createMap("resources/map/floor_tileset.png", 61, 32f, 32f);
        sprite_position = new Vector3f(0, 0, 0);
        sprite_scale = new Vector3f(64f, 64f, 1f);
    }

    @Override
    public void onUserUpdate(float deltaTime) {
        // CAMERA CONTROL
        if (getInput().isKeyDown(GLFW_KEY_UP)) {
            cam_y -= 32 * deltaTime * camera_speed;
        }
        if (getInput().isKeyDown(GLFW_KEY_DOWN)) {
            cam_y += 32 * deltaTime * camera_speed;
        }
        if (getInput().isKeyDown(GLFW_KEY_LEFT)) {
            cam_x += 32 * deltaTime * camera_speed;
        }
        if (getInput().isKeyDown(GLFW_KEY_RIGHT)) {
            cam_x -= 32 * deltaTime * camera_speed;
        }
        setCamera(cam_x, cam_y);
        // SPRITE MOVEMENT
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
        // DEBUG
        if (getInputSystem().isKeyPressed(GLFW_KEY_F1)) {
            System.out.println("PLAYER_X: " + sprite_position.x);
            System.out.println("PLAYER_Y: " + sprite_position.y);
            System.out.println("CAM_X: " + cam_x);
            System.out.println("CAM_Y: " + cam_y);
        }
    }

    @Override
    public void onGfxUpdate(float deltaTime) {
        drawMap(map, 0, 0, 32, 32, 0, 0, 8, 8);
        drawSprite(sprite, (int) sprite_position.x, (int) sprite_position.y, (int) sprite_scale.x, (int) sprite_scale.y);
    }

    public static void launch(String[] args) {
        Window window = new Window(width, height);
        long windowId = window.create();
        Simple2DGame game = new Simple2DGame("Simple 2D Game", window);
        InputSystem input = new InputSystem(game, windowId);
        game.addSystem(input);
        game.addSystem(new RenderSystem(game) {
        });
        GameEngine.initGame(game);
    }
}