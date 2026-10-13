package com.farias.rengine.toolkit2d;

import org.joml.Matrix4f;

import com.farias.rengine.io.Window;
import com.farias.rengine.renderer.RenderPassDescriptor;
import com.farias.rengine.renderer.RenderPassNode;
import com.farias.rengine.renderer.RenderView;
import com.farias.rengine.renderer.Renderer;

/**
 * O que {@link Canvas} e {@link Ui} tem em comum: um pass 2D por frame, em
 * unidades virtuais (origem no canto superior esquerdo, Y para baixo), que
 * recebe sprites e texto. O que muda entre os dois e o descritor do pass
 * (o Canvas limpa a tela, a Ui nao) e os nomes da API de cada um.
 *
 * <p>Detalhe interno do modulo: o jogo usa Canvas ou Ui, criados por
 * {@link Toolkit2D}.
 */
abstract sealed class Layer2D permits Canvas, Ui {
    private final String name;
    private final Renderer renderer;
    private final Window window;
    private final SpriteRenderer sprites;
    private final BitmapFont defaultFont;
    private final int virtualWidth;
    private final int virtualHeight;
    private final Matrix4f projection;
    private final Matrix4f view = new Matrix4f();
    private final RenderPassDescriptor descriptor;

    private RenderPassNode pass;
    private long passFrame = -1;

    Layer2D(String name, Toolkit2D toolkit, int virtualWidth, int virtualHeight, RenderPassDescriptor descriptor) {
        if (virtualWidth <= 0 || virtualHeight <= 0) {
            throw new IllegalArgumentException("virtual size must be positive: " + virtualWidth + "x" + virtualHeight);
        }
        this.name = name;
        this.renderer = toolkit.renderer();
        this.window = toolkit.window();
        this.sprites = toolkit.sprites();
        this.defaultFont = toolkit.defaultFont();
        this.virtualWidth = virtualWidth;
        this.virtualHeight = virtualHeight;
        this.projection = SpriteRenderer.orthographic(virtualWidth, virtualHeight);
        this.descriptor = descriptor;
    }

    /** Abre o pass deste frame. Exige renderer.beginFrame() antes. */
    public void begin() {
        if (!renderer.inFrame()) {
            throw new IllegalStateException(name + ".begin() called outside a frame: call renderer.beginFrame() first");
        }
        RenderView renderView = new RenderView(window.getFramebufferWidth(), window.getFramebufferHeight(),
                projection, view);
        pass = renderer.createPass(descriptor, renderView);
        passFrame = renderer.frameIndex();
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

    /**
     * Converte um X em pixels logicos da janela (ex.: {@code Input.getMouseX()})
     * para unidades virtuais desta superficie.
     */
    public float toVirtualX(double windowX) {
        return (float) (windowX * virtualWidth / Math.max(window.getWidth(), 1));
    }

    /** Idem, para Y (ex.: {@code Input.getMouseY()}). */
    public float toVirtualY(double windowY) {
        return (float) (windowY * virtualHeight / Math.max(window.getHeight(), 1));
    }

    final void sprite(SpriteSheet sheet, int tile, float x, float y, float w, float h, float rotationDeg) {
        sprites.sprite(currentPass(), sheet, tile, x, y, w, h, rotationDeg);
    }

    final RenderPassDescriptor descriptor() {
        return descriptor;
    }

    private RenderPassNode currentPass() {
        if (pass == null || passFrame != renderer.frameIndex()) {
            throw new IllegalStateException(name + ".begin() must be called in this frame before drawing");
        }
        return pass;
    }
}
