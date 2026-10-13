package com.farias.rengine.toolkit2d;

import com.farias.rengine.renderer.RenderPassDescriptor;

/**
 * Superficie 2D de um jogo: um pass que limpa a tela e recebe sprites e
 * texto. Coordenadas em unidades virtuais (o tamanho dado a
 * {@link Toolkit2D#canvas}), origem no canto superior esquerdo, Y para baixo,
 * rotacao em graus no sentido horario.
 *
 * <p>Uso por frame, depois de {@code renderer.beginFrame()}:
 * {@code canvas.begin(); ... spr/text ...; renderer.endFrame();}.
 * Para sobreposicao sobre uma cena 3D (HUD) use {@link Ui}, que nao limpa a tela.
 */
public final class Canvas extends Layer2D {

    Canvas(Toolkit2D toolkit, int virtualWidth, int virtualHeight) {
        super("Canvas", toolkit, virtualWidth, virtualHeight, new RenderPassDescriptor("canvas")
                .clearColor(0f, 0f, 0f, 1f)
                .depthTest(false)
                .cullFace(false)
                .blend(true));
    }

    /** Cor de fundo aplicada ao limpar a tela no inicio do pass. */
    public Canvas clearColor(float r, float g, float b) {
        descriptor().clearColor(r, g, b, 1f);
        return this;
    }

    /** Desenha o tile da folha em (x, y, w, h). */
    public void spr(SpriteSheet sheet, int tile, float x, float y, float w, float h) {
        sprite(sheet, tile, x, y, w, h, 0f);
    }

    /** Idem, girando rotationDeg graus (horario) em torno do centro. */
    public void spr(SpriteSheet sheet, int tile, float x, float y, float w, float h, float rotationDeg) {
        sprite(sheet, tile, x, y, w, h, rotationDeg);
    }
}
