package com.farias.rengine.engine.modules;

import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Module;
import com.farias.rengine.renderer.OpenGLBackend;
import com.farias.rengine.renderer.Renderer;

/**
 * Cria o Renderer modular (com o backend OpenGL). Precisa de um contexto GL
 * ja corrente, ou seja, deve ser registrado depois de {@link WindowModule}.
 */
public final class RenderModule implements Module {
    private Renderer renderer;

    @Override
    public void onRegister(EngineContext context) {
        renderer = new Renderer(new OpenGLBackend());
        renderer.initialize();
        context.registerService(Renderer.class, renderer);
    }

    @Override
    public void onTerminate(EngineContext context) {
        renderer.dispose();
    }
}
