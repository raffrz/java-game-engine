package com.farias.rengine.platform.glfw;

import static org.lwjgl.glfw.GLFW.glfwTerminate;

import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Module;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Window;

/**
 * Cria a janela GLFW (com seu contexto corrente) e o input, e os registra
 * como {@link Window} e {@link InputSystem}. Deve ser o primeiro modulo
 * registrado: os demais dependem da janela ja resolvida.
 */
public final class GlfwModule implements Module {
    private final int width;
    private final int height;
    private final String title;
    private GlfwWindow window;

    public GlfwModule(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    @Override
    public void onRegister(EngineContext context) {
        window = new GlfwWindow(width, height);
        window.create();
        window.setTitle(title);
        window.bind();

        context.registerService(Window.class, window);
        context.registerService(InputSystem.class, new GlfwInput(window));
    }

    /**
     * Roda depois dos modulos que dependem da janela (ordem inversa do
     * registro), entao o Renderer ja liberou seus recursos com o contexto
     * ainda vivo.
     */
    @Override
    public void onTerminate(EngineContext context) {
        if (window != null) {
            window.destroy();
            glfwTerminate();
        }
    }
}
