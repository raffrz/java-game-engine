package com.farias.jengine.renderer;

import org.lwjgl.opengl.GL;

import com.farias.jengine.core.EngineContext;
import com.farias.jengine.core.Module;
import com.farias.jengine.platform.Window;

/**
 * Cria o Renderer modular (OpenGL). Precisa de um contexto GL ja corrente
 * (a Window registrada pelo modulo de plataforma, com o contexto tornado corrente), e
 * inicializa as capabilities do LWJGL para esse contexto.
 */
public final class RendererModule implements Module {
    private Renderer renderer;

    @Override
    public void onStart(EngineContext context) {
        Window window = context.require(Window.class);

        GL.createCapabilities();
        renderer = new Renderer();
        renderer.initialize();
        renderer.setPresent(window::swapBuffers);
        context.registerService(Renderer.class, renderer);
    }

    @Override
    public void onTerminate(EngineContext context) {
        if (renderer != null) {
            renderer.dispose();
        }
    }
}
