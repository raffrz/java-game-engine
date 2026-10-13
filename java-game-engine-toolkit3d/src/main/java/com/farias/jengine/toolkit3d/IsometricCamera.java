package com.farias.jengine.toolkit3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.farias.jengine.platform.Window;
import com.farias.jengine.renderer.RenderView;

/**
 * Câmera ortográfica isométrica: olha para {@link #target()} sempre da mesma
 * diagonal (+X, +Y, +Z), a {@code offset} unidades de distância em cada eixo, e
 * enquadra {@code size} unidades de mundo de meia-altura.
 *
 * <p>
 * Gera a {@link RenderView} do passe 3D e o {@link Ray} sob o mouse a partir
 * da mesma matriz, então o que é desenhado e o que é clicado coincidem.
 */
public final class IsometricCamera {

    private static final float MIN_SIZE = 0.5f;
    private static final float INVERSE_SQRT_2 = (float) (1.0 / Math.sqrt(2.0));
    private static final Vector3f TOWARD_EYE = new Vector3f(1f, 1f, 1f).normalize();

    private final Vector3f target = new Vector3f();
    private final float offset;
    private float size;

    public IsometricCamera(float offset, float size) {
        this.offset = offset;
        this.size = Math.max(size, MIN_SIZE);
    }

    /** O ponto observado; mutável, mova-o diretamente. */
    public Vector3f target() {
        return target;
    }

    /** Meia-altura do enquadramento, em unidades de mundo. */
    public float size() {
        return size;
    }

    /**
     * Move o alvo: {@code right} unidades para a direita da tela (ao longo da
     * diagonal do chão, no plano XZ) e {@code up} unidades para cima no eixo Y
     * do mundo. Valores negativos movem para a esquerda e para baixo.
     */
    public void pan(float right, float up) {
        float alongAxis = right * INVERSE_SQRT_2;
        target.x += alongAxis;
        target.z -= alongAxis;
        target.y += up;
    }

    /** Soma {@code delta} ao enquadramento: positivo afasta, negativo aproxima. */
    public void zoom(float delta) {
        size = Math.max(size + delta, MIN_SIZE);
    }

    /**
     * Direção unitária da superfície para a câmera. É constante em projeção
     * ortográfica, e serve de {@code viewDirection} do brilho especular.
     */
    public Vector3f directionToEye() {
        return new Vector3f(TOWARD_EYE);
    }

    public Matrix4f viewMatrix() {
        return new Matrix4f().lookAt(
                target.x + offset, target.y + offset, target.z + offset,
                target.x, target.y, target.z,
                0f, 1f, 0f);
    }

    /** A view do passe 3D: viewport no tamanho do framebuffer, aspecto da janela lógica. */
    public RenderView view(Window window) {
        // janela minimizada tem altura 0
        float aspect = (float) window.getWidth() / Math.max(window.getHeight(), 1);
        return new RenderView(window.getFramebufferWidth(), window.getFramebufferHeight(),
                Projections.orthographic3D(size, aspect), viewMatrix());
    }

    /**
     * O raio, no mundo, sob a posição do mouse.
     *
     * @param mouseX pixels lógicos da janela, origem no canto superior esquerdo
     */
    public Ray rayAt(double mouseX, double mouseY, Window window) {
        return Picking.ray(view(window), Picking.toNdc(mouseX, mouseY, window.getWidth(), window.getHeight()));
    }
}
