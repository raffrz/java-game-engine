package com.farias.rengine.toolkit2d;

import org.joml.Matrix4f;

import com.farias.rengine.io.Window;
import com.farias.rengine.renderer.RenderPassDescriptor;
import com.farias.rengine.renderer.RenderPassNode;
import com.farias.rengine.renderer.RenderView;
import com.farias.rengine.renderer.Renderer;
import com.farias.rengine.toolkit2d.draw.SpriteRenderer;

/**
 * Superficie 2D de um jogo: um pass que limpa a tela e recebe sprites e
 * texto. Coordenadas em unidades virtuais (largura x altura do
 * {@link Toolkit2DModule}), origem no canto superior esquerdo, Y para baixo,
 * rotacao em graus no sentido horario.
 *
 * <p>Uso por frame, depois de {@code renderer.beginFrame()}:
 * {@code canvas.begin(); ... spr/text ...; renderer.endFrame();}.
 * Para sobreposicao sobre uma cena 3D (HUD) use Ui, que nao limpa a tela.
 */
public final class Canvas {
    private final Renderer renderer;
    private final Window window;
    private final SpriteRenderer sprites;
    private final BitmapFont defaultFont;
    private final Matrix4f projection;
    private final Matrix4f view = new Matrix4f();
    private final RenderPassDescriptor descriptor;

    private RenderPassNode pass;
    private long passFrame = -1;

    Canvas(Renderer renderer, Window window, SpriteRenderer sprites, BitmapFont defaultFont, int virtualWidth,
            int virtualHeight) {
        this.renderer = renderer;
        this.window = window;
        this.sprites = sprites;
        this.defaultFont = defaultFont;
        this.projection = SpriteRenderer.orthographic(virtualWidth, virtualHeight);
        this.descriptor = new RenderPassDescriptor("canvas")
                .clearColor(0f, 0f, 0f, 1f)
                .depthTest(false)
                .cullFace(false)
                .blend(true);
    }

    /** Cor de fundo aplicada ao limpar a tela no inicio do pass. */
    public Canvas clearColor(float r, float g, float b) {
        descriptor.clearColor(r, g, b, 1f);
        return this;
    }

    /** Abre o pass 2D deste frame. Exige renderer.beginFrame() antes. */
    public void begin() {
        if (!renderer.inFrame()) {
            throw new IllegalStateException("Canvas.begin() called outside a frame: call renderer.beginFrame() first");
        }
        RenderView renderView = new RenderView(window.getFramebufferWidth(), window.getFramebufferHeight(),
                projection, view);
        pass = renderer.createPass(descriptor, renderView);
        passFrame = renderer.frameIndex();
    }

    /** Desenha o tile da folha em (x, y, w, h). */
    public void spr(SpriteSheet sheet, int tile, float x, float y, float w, float h) {
        spr(sheet, tile, x, y, w, h, 0f);
    }

    /** Idem, girando rotationDeg graus (horario) em torno do centro. */
    public void spr(SpriteSheet sheet, int tile, float x, float y, float w, float h, float rotationDeg) {
        sprites.sprite(currentPass(), sheet, tile, x, y, w, h, rotationDeg);
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
            throw new IllegalStateException("Canvas.begin() must be called in this frame before drawing");
        }
        return pass;
    }
}
