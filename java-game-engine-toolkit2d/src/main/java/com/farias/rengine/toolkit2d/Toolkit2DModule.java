package com.farias.rengine.toolkit2d;

import com.farias.rengine.assets.Assets;
import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Module;
import com.farias.rengine.io.Window;
import com.farias.rengine.renderer.Renderer;

/**
 * Registra os servicos 2D: {@link Toolkit2D} (fabrica de {@link Canvas} e
 * {@link Ui}) e o {@link SpriteRenderer} de baixo nivel. Deve vir depois do
 * modulo de plataforma (GlfwModule), RenderModule e AssetsModule.
 */
public final class Toolkit2DModule implements Module {
    private static final String DEFAULT_FONT = "fonts/default16x16.png";
    private static final int DEFAULT_GLYPH_SIZE = 16;

    private SpriteRenderer sprites;

    @Override
    public void onRegister(EngineContext context) {
        Renderer renderer = context.require(Renderer.class);
        Window window = context.require(Window.class);
        Assets assets = context.require(Assets.class);

        sprites = new SpriteRenderer(renderer);
        BitmapFont font = new BitmapFont(assets.texture(DEFAULT_FONT), DEFAULT_GLYPH_SIZE, DEFAULT_GLYPH_SIZE);

        context.registerService(SpriteRenderer.class, sprites);
        context.registerService(Toolkit2D.class, new Toolkit2D(renderer, window, sprites, font));
    }

    @Override
    public void onTerminate(EngineContext context) {
        if (sprites != null) {
            sprites.clear();
        }
    }
}
