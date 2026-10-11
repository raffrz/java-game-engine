package com.farias.rengine.assets;

import java.nio.file.Path;
import java.util.Objects;

import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Module;
import com.farias.rengine.renderer.Renderer;

/**
 * Registra o servico {@link Assets}. Deve vir depois de RenderModule, ja que
 * a carga de texturas passa pelo Renderer.
 */
public final class AssetsModule implements Module {
    private final Path root;
    private Assets assets;

    public AssetsModule(Path root) {
        this.root = Objects.requireNonNull(root, "root");
    }

    @Override
    public void onRegister(EngineContext context) {
        Renderer renderer = context.require(Renderer.class);
        assets = new Assets(renderer, root);
        context.registerService(Assets.class, assets);
    }

    @Override
    public void onTerminate(EngineContext context) {
        if (assets != null) {
            assets.clear();
        }
    }
}
