package com.farias.rengine.engine3d;

import com.farias.rengine.assets.Assets;
import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Module;
import com.farias.rengine.renderer.Renderer;

/**
 * Registra os componentes 3D que dependem de servicos (Meshes, Materials,
 * ModelLoader), para o jogo resolver por {@code context.require(...)} em vez
 * de instancia-los. Deve vir depois de RenderModule e AssetsModule. Camera,
 * TexturedModel e Projections nao dependem de servico e seguem como estao.
 */
public final class Engine3DModule implements Module {
    @Override
    public void onRegister(EngineContext context) {
        Renderer renderer = context.require(Renderer.class);
        Assets assets = context.require(Assets.class);

        Meshes meshes = new Meshes(renderer);
        Materials materials = new Materials(renderer);
        ModelLoader modelLoader = new ModelLoader(assets, renderer, materials);

        context.registerService(Meshes.class, meshes);
        context.registerService(Materials.class, materials);
        context.registerService(ModelLoader.class, modelLoader);
    }
}
