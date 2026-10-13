package com.farias.rengine.toolkit3d;

import com.farias.rengine.assets.Assets;
import com.farias.rengine.core.EngineContext;
import com.farias.rengine.core.Module;
import com.farias.rengine.renderer.Renderer;

/**
 * Registra os componentes 3D que dependem de servicos ({@link Shapes},
 * {@link LitMaterials}, {@link Shadows}, {@link Models}), para o jogo
 * resolver por {@code context.require(...)} em vez de instancia-los. Deve vir
 * depois de RendererModule e AssetsModule. Camera, IsometricCamera,
 * TexturedModel, DirectionalLight, ShapeData, Picking e Projections nao
 * dependem de servico e seguem como estao.
 */
public final class Toolkit3DModule implements Module {
    private Models models;

    @Override
    public void onStart(EngineContext context) {
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
