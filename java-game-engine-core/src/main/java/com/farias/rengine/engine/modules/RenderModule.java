package com.farias.rengine.engine.modules;

import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Module;
import com.farias.rengine.io.Window;
import com.farias.rengine.renderer.Renderer;

/**
 * Cria o Renderer modular. Precisa de um contexto GL ja corrente, ou seja,
 * exige a Window registrada por {@link WindowModule}.
 */
public final class RenderModule implements Module {
    private Renderer renderer;

    @Override
    public void onRegister(EngineContext context) {
        Window window = context.require(Window.class);

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
