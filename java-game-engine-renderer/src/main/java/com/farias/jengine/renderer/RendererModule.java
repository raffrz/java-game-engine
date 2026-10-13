package com.farias.jengine.renderer;

import org.lwjgl.opengl.GL;

import com.farias.jengine.core.EngineContext;
import com.farias.jengine.core.Module;
import com.farias.jengine.platform.Presenter;

/**
 * Cria o Renderer modular (OpenGL). Precisa de um contexto GL ja corrente na
 * thread e de um {@link Presenter}, ambos fornecidos pelo modulo de
 * plataforma (que por isso vem antes), e inicializa as capabilities do LWJGL
 * para esse contexto. Nao conhece a janela: so o contrato de apresentacao.
 */
public final class RendererModule implements Module {
    private Renderer renderer;

    @Override
    public void onStart(EngineContext context) {
        Presenter presenter = context.require(Presenter.class);

        GL.createCapabilities();
        renderer = new Renderer(presenter);
        renderer.initialize();
        context.registerService(Renderer.class, renderer);
    }

    @Override
    public void onTerminate(EngineContext context) {
        if (renderer != null) {
            renderer.dispose();
        }
    }
}
