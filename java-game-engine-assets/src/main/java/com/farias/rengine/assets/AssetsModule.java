package com.farias.rengine.assets;

import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Module;
import com.farias.rengine.renderer.Renderer;

/**
 * Registra o servico {@link Assets}. Deve vir depois de RenderModule, ja que
 * a carga de texturas passa pelo Renderer.
 */
public final class AssetsModule implements Module {
    @Override
    public void onRegister(EngineContext context) {
        context.require(Renderer.class);
        context.registerService(Assets.class, new Assets());
    }
}
