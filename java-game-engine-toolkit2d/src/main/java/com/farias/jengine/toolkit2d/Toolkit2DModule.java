package com.farias.jengine.toolkit2d;

import com.farias.jengine.assets.Assets;
import com.farias.jengine.core.EngineContext;
import com.farias.jengine.core.Module;
import com.farias.jengine.platform.Window;
import com.farias.jengine.renderer.Renderer;

/**
 * Registra os servicos 2D: {@link Toolkit2D} (fabrica de {@link Canvas} e
 * {@link Ui}) e o {@link SpriteRenderer} de baixo nivel. Deve vir depois do
 * modulo de plataforma (GlfwPlatformModule), RendererModule e AssetsModule.
 */
public final class Toolkit2DModule implements Module {
    private static final String DEFAULT_FONT = "fonts/default16x16.png";
    private static final int DEFAULT_GLYPH_SIZE = 16;

    private SpriteRenderer sprites;

    @Override
    public void onStart(EngineContext context) {
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
