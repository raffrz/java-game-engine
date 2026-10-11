package com.farias.rengine.toolkit2d;

import com.farias.rengine.assets.Assets;
import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Module;
import com.farias.rengine.io.Window;
import com.farias.rengine.renderer.Renderer;
import com.farias.rengine.toolkit2d.draw.SpriteRenderer;

/**
 * Registra os servicos 2D: {@link Canvas} e o {@link SpriteRenderer} de baixo
 * nivel. Deve vir depois de WindowModule, RenderModule e AssetsModule.
 *
 * <p>O tamanho virtual e o espaco de coordenadas que o jogo usa; ele e
 * escalado para o framebuffer da janela.
 */
public final class Toolkit2DModule implements Module {
    private static final String DEFAULT_FONT = "fonts/default16x16.png";
    private static final int DEFAULT_GLYPH_SIZE = 16;

    private final int virtualWidth;
    private final int virtualHeight;
    private SpriteRenderer sprites;

    public Toolkit2DModule(int virtualWidth, int virtualHeight) {
        if (virtualWidth <= 0 || virtualHeight <= 0) {
            throw new IllegalArgumentException("virtual size must be positive: " + virtualWidth + "x" + virtualHeight);
        }
        this.virtualWidth = virtualWidth;
        this.virtualHeight = virtualHeight;
    }

    @Override
    public void onRegister(EngineContext context) {
        Renderer renderer = context.require(Renderer.class);
        Window window = context.require(Window.class);
        Assets assets = context.require(Assets.class);

        sprites = new SpriteRenderer(renderer);
        BitmapFont font = new BitmapFont(assets.texture(DEFAULT_FONT), DEFAULT_GLYPH_SIZE, DEFAULT_GLYPH_SIZE);
        Canvas canvas = new Canvas(renderer, window, sprites, font, virtualWidth, virtualHeight);

        context.registerService(SpriteRenderer.class, sprites);
        context.registerService(Canvas.class, canvas);
    }

    @Override
    public void onTerminate(EngineContext context) {
        if (sprites != null) {
            sprites.clear();
        }
    }
}
