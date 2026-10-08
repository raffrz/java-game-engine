package com.farias.rengine;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

import java.util.ArrayList;
import java.util.List;

import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Window;

/**
 * Dono do game loop. A renderizacao em si (passes, camera, projecao) e
 * gerenciada pela fachada {@link GameEngine}, que por sua vez delega para o
 * Renderer modular.
 */
public abstract class Game {
    private final String title;
    private final Window window;
    private final long msPerFrame = 1000 / 60;
    private final boolean frameLimiter;
    private InputSystem inputSystem;
    private final List<System> systems = new ArrayList<>();

    public Game(String title, Window window) {
        this(title, window, false);
    }

    public Game(String title, Window window, boolean limitFPS) {
        this.title = title;
        this.window = window;
        this.frameLimiter = limitFPS;
    }

    public void init() {
        window.bind();
        GameEngine.initRenderer(window.getWidth(), window.getHeight());
        this.onUserCreate();
    }

    public void run() {
        init();

        long start = java.lang.System.currentTimeMillis();
        long fpsTime = 0;
        long updateTime = 0;
        int frameCount = 0;

        while (!window.shouldClose()) {
            if (inputSystem.isKeyPressed(GLFW_KEY_ESCAPE)) {
                break;
            }

            long current = java.lang.System.currentTimeMillis();
            long elapsed = current - start;
            start = current;
            fpsTime += elapsed;
            updateTime += elapsed;

            if (fpsTime > 1000) {
                window.setTitle(title + " - FPS: " + frameCount);
                frameCount = 0;
                fpsTime = 0;
            }

            float deltaTime = updateTime / 1000f;
            if (updateTime > msPerFrame) {
                inputSystem.update(this, deltaTime);
                for (System s : systems) {
                    s.update(this, deltaTime);
                }
                this.onUserUpdate(deltaTime);
                updateTime = 0;
            }

            GameEngine.beginFrame();
            GameEngine.setState(GameState.SCENE);
            this.onGfxUpdate(deltaTime);
            GameEngine.setState(GameState.UI);
            this.onUIUpdate(deltaTime);
            GameEngine.endFrame();

            window.swapBuffers(frameLimiter);
            frameCount++;
        }

        GameEngine.dispose();
        glfwTerminate();
    }

    public abstract void onUserCreate();

    public abstract void onUserUpdate(float deltaTime);

    public void onGfxUpdate(float deltaTime) {
    }

    public void onUIUpdate(float deltaTime) {
    }

    public void addSystem(System system) {
        if (system instanceof InputSystem is) {
            this.inputSystem = is;
        } else {
            this.systems.add(system);
        }
    }

    public InputSystem getInputSystem() {
        return inputSystem;
    }

    public Window getWindow() {
        return window;
    }
}
