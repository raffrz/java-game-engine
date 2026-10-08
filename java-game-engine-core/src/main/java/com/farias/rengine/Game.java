package com.farias.rengine;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

import com.farias.rengine.engine.Engine;
import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Timer;
import com.farias.rengine.engine.modules.InputModule;
import com.farias.rengine.engine.modules.RenderModule;
import com.farias.rengine.engine.modules.WindowModule;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Window;
import com.farias.rengine.renderer.Renderer;

/**
 * Casca fina sobre Engine/EngineContext/Timer: monta a janela, o input e o
 * renderer via modulos, e repassa os callbacks de ciclo de vida do jogo. Nao
 * ha GameState nem passes fixos aqui - cada Game decide, em onRender, quais
 * passes criar e submeter (ver Renderer/RenderPassDescriptor), do mesmo jeito
 * que o renderer/example/Example.java demonstra.
 */
public abstract class Game {
    private final String title;
    private final int width;
    private final int height;

    protected Engine engine;
    protected EngineContext context;
    protected Window window;
    protected InputSystem input;
    protected Renderer renderer;
    protected Timer timer;

    protected Game(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
    }

    public final void run() {
        engine = new Engine();
        engine.registerModule(new WindowModule(width, height, title));
        engine.registerModule(new InputModule());
        engine.registerModule(new RenderModule());
        context = engine.start();

        window = context.resolve(Window.class);
        input = context.resolve(InputSystem.class);
        renderer = context.resolve(Renderer.class);
        timer = new Timer();

        onCreate();

        while (!window.shouldClose()) {
            if (input.isKeyPressed(GLFW_KEY_ESCAPE)) {
                break;
            }

            timer.update();
            float deltaTime = timer.deltaTime();

            input.update(this, deltaTime);
            onUpdate(deltaTime);

            renderer.beginFrame();
            onRender(deltaTime);
            renderer.endFrame();

            window.swapBuffers();
        }

        engine.terminate();
        glfwTerminate();
    }

    protected abstract void onCreate();

    protected abstract void onUpdate(float deltaTime);

    protected abstract void onRender(float deltaTime);
}
