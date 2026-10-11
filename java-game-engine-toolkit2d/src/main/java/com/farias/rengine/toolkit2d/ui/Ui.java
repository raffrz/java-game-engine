package com.farias.rengine.toolkit2d.ui;

import org.joml.Matrix4f;

import com.farias.rengine.io.Window;
import com.farias.rengine.renderer.RenderPassDescriptor;
import com.farias.rengine.renderer.RenderPassNode;
import com.farias.rengine.renderer.RenderView;
import com.farias.rengine.renderer.Renderer;
import com.farias.rengine.toolkit2d.BitmapFont;
import com.farias.rengine.toolkit2d.SpriteSheet;
import com.farias.rengine.toolkit2d.draw.SpriteRenderer;

/**
 * Camada de interface sobreposta: desenha texto e imagens em espaco de tela
 * (unidades virtuais, origem no canto superior esquerdo, Y para baixo) sem
 * limpar nada, entao pode ir por cima de uma cena 3D ou de um Canvas.
 *
 * <p>Os passes executam na ordem de criacao: chame {@link #begin()} depois de
 * criar o pass da cena. Depende so de {@code draw} e dos tipos de valor
 * SpriteSheet/BitmapFont, nunca de Canvas, para poder virar um modulo proprio.
 */
public final class Ui {
    private final Renderer renderer;
    private final Window window;
    private final SpriteRenderer sprites;
    private final BitmapFont defaultFont;
    private final Matrix4f projection;
    private final Matrix4f view = new Matrix4f();
    private final RenderPassDescriptor descriptor;

    private RenderPassNode pass;
    private long passFrame = -1;

    public Ui(Renderer renderer, Window window, SpriteRenderer sprites, BitmapFont defaultFont, int virtualWidth,
            int virtualHeight) {
        this.renderer = renderer;
        this.window = window;
        this.sprites = sprites;
        this.defaultFont = defaultFont;
        this.projection = SpriteRenderer.orthographic(virtualWidth, virtualHeight);
        this.descriptor = new RenderPassDescriptor("ui")
                .clearColorBuffer(false)
                .clearDepthBuffer(false)
                .depthTest(false)
                .cullFace(false)
                .blend(true);
    }

    /** Abre o pass de UI deste frame. Exige renderer.beginFrame() antes. */
    public void begin() {
        if (!renderer.inFrame()) {
            throw new IllegalStateException("Ui.begin() called outside a frame: call renderer.beginFrame() first");
        }
        RenderView renderView = new RenderView(window.getFramebufferWidth(), window.getFramebufferHeight(),
                projection, view);
        pass = renderer.createPass(descriptor, renderView);
        passFrame = renderer.frameIndex();
    }

    /** Desenha o tile da folha em (x, y, w, h). */
    public void image(SpriteSheet sheet, int tile, float x, float y, float w, float h) {
        sprites.sprite(currentPass(), sheet, tile, x, y, w, h, 0f);
    }

    /** Texto alinhado a esquerda em x; tamanho do glifo da fonte. */
    public void text(BitmapFont font, String text, float x, float y) {
        text(font, text, x, y, font.glyphH());
    }

    /** Idem, com size = altura do glifo em unidades virtuais. */
    public void text(BitmapFont font, String text, float x, float y, float size) {
        sprites.text(currentPass(), font, text, x, y, size);
    }

    /** Texto centrado em x; tamanho do glifo da fonte. */
    public void textCentered(BitmapFont font, String text, float x, float y) {
        textCentered(font, text, x, y, font.glyphH());
    }

    /** Idem, com size = altura do glifo em unidades virtuais. */
    public void textCentered(BitmapFont font, String text, float x, float y, float size) {
        sprites.textCentered(currentPass(), font, text, x, y, size);
    }

    /** Fonte 16x16 embutida (fonts/default16x16.png). */
    public BitmapFont defaultFont() {
        return defaultFont;
    }

    private RenderPassNode currentPass() {
        if (pass == null || passFrame != renderer.frameIndex()) {
            throw new IllegalStateException("Ui.begin() must be called in this frame before drawing");
        }
        return pass;
    }
}
