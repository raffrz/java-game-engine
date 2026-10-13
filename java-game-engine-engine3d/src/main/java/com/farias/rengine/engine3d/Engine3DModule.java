package com.farias.rengine.engine3d;

import com.farias.rengine.assets.Assets;
import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Module;
import com.farias.rengine.renderer.Renderer;

/**
 * Registra os componentes 3D que dependem de servicos ({@link Shapes},
 * {@link LitMaterials}, {@link Shadows}, {@link Models}), para o jogo
 * resolver por {@code context.require(...)} em vez de instancia-los. Deve vir
 * depois de RenderModule e AssetsModule. Camera, IsometricCamera,
 * TexturedModel, DirectionalLight, ShapeData, Picking e Projections nao
 * dependem de servico e seguem como estao.
 */
public final class Engine3DModule implements Module {
    private Models models;

    @Override
    public void onRegister(EngineContext context) {
        Renderer renderer = context.require(Renderer.class);
        Assets assets = context.require(Assets.class);

        LitMaterials materials = new LitMaterials(renderer);
        models = new Models(assets, renderer, materials);

        context.registerService(Shapes.class, new Shapes(renderer));
        context.registerService(LitMaterials.class, materials);
        context.registerService(Shadows.class, new Shadows(renderer));
        context.registerService(Models.class, models);
    }

    @Override
    public void onTerminate(EngineContext context) {
        if (models != null) {
            models.clear();
        }
    }
}
