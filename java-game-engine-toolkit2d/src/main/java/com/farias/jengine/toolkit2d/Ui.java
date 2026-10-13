package com.farias.jengine.toolkit2d;

import com.farias.jengine.renderer.RenderPassDescriptor;

/**
 * Camada de interface sobreposta: desenha texto e imagens em espaco de tela
 * (unidades virtuais, origem no canto superior esquerdo, Y para baixo) sem
 * limpar nada, entao pode ir por cima de uma cena 3D ou de um {@link Canvas}.
 *
 * <p>Os passes executam na ordem de criacao: chame {@link #begin()} depois de
 * criar o pass da cena.
 */
public final class Ui extends Layer2D {

    Ui(Toolkit2D toolkit, int virtualWidth, int virtualHeight) {
        super("Ui", toolkit, virtualWidth, virtualHeight, new RenderPassDescriptor("ui")
                .clearColorBuffer(false)
                .clearDepthBuffer(false)
                .depthTest(false)
                .cullFace(false)
                .blend(true));
    }

    /** Desenha o tile da folha em (x, y, w, h). */
    public void image(SpriteSheet sheet, int tile, float x, float y, float w, float h) {
        sprite(sheet, tile, x, y, w, h, 0f);
    }
}
