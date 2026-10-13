package com.farias.jengine.platform.glfw;

import static org.lwjgl.glfw.GLFW.glfwTerminate;

import com.farias.jengine.core.EngineContext;
import com.farias.jengine.core.Module;
import com.farias.jengine.platform.Input;
import com.farias.jengine.platform.Window;

/**
 * Cria a janela GLFW (com seu contexto corrente) e o input, e os registra
 * como {@link Window} e {@link Input}. Deve ser o primeiro modulo
 * registrado: os demais dependem da janela ja resolvida.
 */
public final class GlfwPlatformModule implements Module {
    private final int width;
    private final int height;
    private final String title;
    private GlfwWindow window;

    public GlfwPlatformModule(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    @Override
    public void onStart(EngineContext context) {
        window = new GlfwWindow(width, height);
        window.create();
        window.setTitle(title);
        window.bind();

        context.registerService(Window.class, window);
        context.registerService(Input.class, new GlfwInput(window));
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
