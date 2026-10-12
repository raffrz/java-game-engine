package com.farias.rengine.renderer;

import org.lwjgl.opengl.GL;

import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Module;
import com.farias.rengine.io.Window;

/**
 * Cria o Renderer modular (OpenGL). Precisa de um contexto GL ja corrente
 * (a Window registrada por WindowModule, com o contexto tornado corrente), e
 * inicializa as capabilities do LWJGL para esse contexto.
 */
public final class RenderModule implements Module {
    private Renderer renderer;

    @Override
    public void onRegister(EngineContext context) {
        Window window = context.require(Window.class);

        GL.createCapabilities();
        renderer = new Renderer();
        renderer.initialize();
        renderer.setPresent(window::swapBuffers);
        context.registerService(Renderer.class, renderer);
    }

    @Override
    public void onTerminate(EngineContext context) {
        renderer.dispose();
    }
}
