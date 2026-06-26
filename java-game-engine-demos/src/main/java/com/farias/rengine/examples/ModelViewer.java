package com.farias.rengine.examples;

import static com.farias.rengine.GameEngine.drawText;
import static com.farias.rengine.GameEngine.drawTexturedModel;
import static com.farias.rengine.GameEngine.getInput;
import static com.farias.rengine.GameEngine.orthographicMode;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Z;

import com.farias.rengine.Game;
import com.farias.rengine.GameEngine;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Window;
import com.farias.rengine.render.RenderSystem;
import com.farias.rengine.render.TexturedModel;

public class ModelViewer extends Game {

    private static final int WINDOW_WIDTH = 960;
    private static final int WINDOW_HEIGHT = 960;
    private static final int FONT_SIZE = 20;
    private static final float CAMERA_SPEED = 5.0f;
    private static final float ROTATION_SPEED = 16f;

    private TexturedModel earth;
    private TexturedModel clouds;

    // ponto observado pela câmera
    private float targetX = 0;
    private float targetY = 0;
    private float targetZ = 0;
    private float fov = 5f;

    // distância da câmera para visão isométrica
    private float cameraDistance = 10f;

    public ModelViewer(Window window) {
        super("Model Viewer - Isometric", window);
    }

    @Override
    public void onUserCreate() {

        // Projeção ortográfica = base da visão isométrica
        orthographicMode(WINDOW_WIDTH, WINDOW_HEIGHT);

        earth = GameEngine.loadTexturedModel("earth");
        clouds = GameEngine.loadTexturedModel("clouds");

        updateIsometricCamera();
    }

    private void updateIsometricCamera() {
        GameEngine.setCameraLookAt(
                targetX + cameraDistance,
                targetY + cameraDistance,
                targetZ + cameraDistance,

                targetX,
                targetY,
                targetZ,

                0, 1, 0);
    }

    @Override
    public void onUserUpdate(float deltaTime) {

        // rotação da terra
        earth.getRotation().y -= 0.7f * deltaTime * ROTATION_SPEED;

        if (earth.getRotation().y < -360f) {
            earth.getRotation().y += 360f;
        }

        // rotação das nuvens
        clouds.getRotation().y -= 1.0f * deltaTime * ROTATION_SPEED;

        if (clouds.getRotation().y < -360f) {
            clouds.getRotation().y += 360f;
        }

        // tamanho da atmosfera
        if (getInput().isKeyDown(GLFW_KEY_A)) {

            if (getInput().isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
                clouds.getScale().sub(0.1f, 0.1f, 0.1f);
            } else {
                clouds.getScale().add(0.1f, 0.1f, 0.1f);
            }
        }

        float move = CAMERA_SPEED * deltaTime;

        /*
         * Movimento isométrico
         *
         * ↑ = nordeste
         * ↓ = sudoeste
         * ← = noroeste
         * → = sudeste
         */

        if (getInput().isKeyDown(GLFW_KEY_UP)) {
            targetX += move;
            targetZ += move;
        }

        if (getInput().isKeyDown(GLFW_KEY_DOWN)) {
            targetX -= move;
            targetZ -= move;
        }

        if (getInput().isKeyDown(GLFW_KEY_LEFT)) {
            targetX -= move;
            targetZ += move;
        }

        if (getInput().isKeyDown(GLFW_KEY_RIGHT)) {
            targetX += move;
            targetZ -= move;
        }

        // sobe/desce a câmera
        if (getInput().isKeyDown(GLFW_KEY_UP)) {
            targetY += move;
        }

        if (getInput().isKeyDown(GLFW_KEY_DOWN)) {
            targetY -= move;
        }

        // zoom
        if (getInput().isKeyDown(GLFW_KEY_Z)) {

            
            if (getInput().isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
                // cameraDistance += move;
                fov += 2f * deltaTime * CAMERA_SPEED;
            } else {
                // cameraDistance -= move;
                fov -= 2f * deltaTime * CAMERA_SPEED;
            }

            if (cameraDistance < 2f) {
                cameraDistance = 2f;
            }
        }

        updateIsometricCamera();
    }

    @Override
    public void onGfxUpdate(float deltaTime) {

        // Renderização 3D ortográfica
        GameEngine.orthographic3DMode(fov, (float)WINDOW_WIDTH / WINDOW_HEIGHT);

        drawTexturedModel(earth);
        drawTexturedModel(clouds);

        GameEngine.orthographicMode(WINDOW_WIDTH, WINDOW_HEIGHT);

        // UI
        drawText(
                "ISOMETRIC VIEW",
                20,
                20,
                FONT_SIZE,
                FONT_SIZE);

        drawText(
                "ARROWS = MOVE | Z = ZOOM | SHIFT+Z = ZOOM OUT",
                20,
                50,
                14,
                14);
    }

    public static void main(String[] args) {

        Window window = new Window(
                WINDOW_WIDTH,
                WINDOW_HEIGHT);

        long windowId = window.create();

        ModelViewer game = new ModelViewer(window);

        game.addSystem(new InputSystem(windowId));
        game.addSystem(new RenderSystem());

        GameEngine.initGame(game);
    }
}
