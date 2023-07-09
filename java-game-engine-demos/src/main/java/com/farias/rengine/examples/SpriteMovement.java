package com.farias.rengine.examples;

import static org.lwjgl.glfw.GLFW.*;

import org.joml.Math;
import org.joml.Vector3f;

import com.farias.rengine.Game;
import com.farias.rengine.GameEngine;
import com.farias.rengine.ecs.ECSGame;
import com.farias.rengine.ecs.GameObject;
import com.farias.rengine.ecs.Transform;
import com.farias.rengine.ecs.gfx.Sprite;
import com.farias.rengine.ecs.gfx.TileMap;
import com.farias.rengine.render.Renderable;

public class SpriteMovement extends ECSGame {
    static final float FLOOR_TILE_SIZE = 32f;
    static final int MAP_SIZE = 4;
    static final String TITLE = "Amorous Adventures";
    static final int FLOOR_1 = 61;
    static final int screen_width = 1920;
    static final int screen_height = 1080;

    GameObject map;
    float rotationX = 0;
    float rotationY = 0;
    float rotationZ = 0;
    float translationX = 0;
    float translationY = 0;
    float translationZ = 0;
    boolean rotationOperation;
    boolean translationOperation;

    public SpriteMovement() {
        super(TITLE, screen_width, screen_height, new TopDownCamera2D(screen_width, screen_height, 0.0f));
    }

    public static void launch(String[] args) {
        Game game = new SpriteMovement();
        GameEngine.initGame(game);
    }

    @Override
    public void onUserCreate() {
        createMap();
        // createPlayer();
    }

    @Override
    public void onUserUpdate(float deltaTime) {
        Transform transform = (Transform) map.getComponent("transform");
        // transform.setPosition(transform.getPosition().add(factor, 0, 0));
        // count++;
        // if (count > 30) {
        // count = 0;
        // factor*=-1;
        // }
        if (getInputSystem().isKeyPressed(GLFW_KEY_R)) {
            translationOperation = false;
            rotationOperation = true;
        }

        if (getInputSystem().isKeyPressed(GLFW_KEY_G)) {
            translationOperation = true;
            rotationOperation = false;
        }

        if (rotationOperation) {
            if (getInputSystem().isKeyDown(GLFW_KEY_X)) {
                if (rotationX++ > 360)
                    rotationX = 0;
            }
            if (getInputSystem().isKeyDown(GLFW_KEY_Y)) {
                if (rotationY++ > 360)
                    rotationY = 0;
            } else if (getInputSystem().isKeyDown(GLFW_KEY_Z)) {
                if (rotationZ++ > 360)
                    rotationZ = 0;
            }
            // this.rotateEntity(map, rotationX, rotationY, rotationZ);
            transform.getRotation().set((float) Math.toRadians(rotationX),
                    (float) Math.toRadians(rotationY), (float) Math.toRadians(rotationZ));
        }

        if (translationOperation) {
            if (getInputSystem().isKeyDown(GLFW_KEY_X))
                translationX--;
            if (getInputSystem().isKeyDown(GLFW_KEY_Y))
                translationY--;
            if (getInputSystem().isKeyDown(GLFW_KEY_Z))
                translationZ--;
            // this.translateEntity(map, translationX, translationY, translationZ);
            transform.getPosition().set(translationX, translationY, translationZ);
        }

        if (getInputSystem().isKeyPressed(GLFW_KEY_ENTER)) {
            translationOperation = false;
            rotationOperation = false;
        }
    }

    void createMap() {
        this.map = new Map();
        this.addEntity(map);
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
}
