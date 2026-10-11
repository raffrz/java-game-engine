package com.farias.rengine.engine.modules;

import static org.lwjgl.glfw.GLFW.glfwTerminate;

import org.lwjgl.opengl.GL;

import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Module;
import com.farias.rengine.io.Window;

/**
 * Cria a janela GLFW, torna o contexto GL corrente e inicializa as
 * capabilities do LWJGL. Deve ser o primeiro modulo registrado, ja que
 * RenderModule/InputModule dependem de um contexto GL corrente e do
 * Window.class ja resolvido.
 */
public final class WindowModule implements Module {
    private final int width;
    private final int height;
    private final String title;
    private Window window;

    public WindowModule(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    @Override
    public void onRegister(EngineContext context) {
        window = new Window(width, height);
        window.create();
        window.setTitle(title);
        window.bind();
        GL.createCapabilities();
        context.registerService(Window.class, window);
    }

    /**
     * Roda depois dos modulos que dependem da janela (ordem inversa do
     * registro), entao o Renderer ja liberou seus recursos com o contexto GL
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
