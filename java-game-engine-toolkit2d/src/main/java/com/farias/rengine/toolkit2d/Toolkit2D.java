package com.farias.rengine.toolkit2d;

import com.farias.rengine.io.Window;
import com.farias.rengine.renderer.Renderer;

/**
 * Fabrica das superficies 2D, registrada por {@link Toolkit2DModule}. O jogo
 * cria quantas quiser, cada uma com o proprio tamanho virtual (por exemplo um
 * Canvas 128x128 para o jogo e uma Ui 640x480 para o HUD), e as guarda: criar
 * uma por frame so desperdica alocacao.
 */
public final class Toolkit2D {
    private final Renderer renderer;
    private final Window window;
    private final SpriteRenderer sprites;
    private final BitmapFont defaultFont;

    Toolkit2D(Renderer renderer, Window window, SpriteRenderer sprites, BitmapFont defaultFont) {
        this.renderer = renderer;
        this.window = window;
        this.sprites = sprites;
        this.defaultFont = defaultFont;
    }

    /**
     * Um Canvas com o espaco de coordenadas virtualWidth x virtualHeight,
     * escalado para o framebuffer da janela.
     */
    public Canvas canvas(int virtualWidth, int virtualHeight) {
        return new Canvas(this, virtualWidth, virtualHeight);
    }

    /** Uma Ui (overlay) com o espaco de coordenadas virtualWidth x virtualHeight. */
    public Ui ui(int virtualWidth, int virtualHeight) {
        return new Ui(this, virtualWidth, virtualHeight);
    }

    /** Fonte 16x16 embutida (fonts/default16x16.png). */
    public BitmapFont defaultFont() {
        return defaultFont;
    }

    Renderer renderer() {
        return renderer;
    }

    Window window() {
        return window;
    }

    SpriteRenderer sprites() {
        return sprites;
    }
}
