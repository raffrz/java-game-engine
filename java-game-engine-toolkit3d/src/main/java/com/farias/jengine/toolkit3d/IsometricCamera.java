package com.farias.jengine.toolkit3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.farias.jengine.platform.Window;
import com.farias.jengine.renderer.RenderView;

/**
 * Câmera ortográfica isométrica: olha para {@link #target()} de cima, na
 * diagonal, a {@code offset} unidades de altura, e enquadra {@code size}
 * unidades de mundo de meia-altura.
 *
 * <p>
 * A câmera orbita em volta do eixo Y do alvo ({@link #rotate}) sem mudar a
 * altura nem a distância. No yaw inicial de 45° o olho fica na diagonal (+X,
 * +Y, +Z), a {@code offset} unidades do alvo em cada eixo.
 *
 * <p>
 * Gera a {@link RenderView} do passe 3D e o {@link Ray} sob o mouse a partir
 * da mesma matriz, então o que é desenhado e o que é clicado coincidem.
 */
public final class IsometricCamera {

    private static final float MIN_SIZE = 0.5f;
    private static final float SQRT_2 = (float) Math.sqrt(2.0);

    /**
     * Yaw inicial, em graus: põe o olho em target + (offset, offset, offset).
     */
    private static final float INITIAL_YAW = 45f;

    private final Vector3f target = new Vector3f();
    private final float offset;
    private float size;

    /**
     * Ângulo horizontal de onde a câmera olha, em graus, em [0, 360): 0° = olho
     * no eixo +Z, 90° = olho no eixo +X. A direção horizontal do olho é
     * (sin(yaw), cos(yaw)) em (x, z).
     */
    private float yaw = INITIAL_YAW;

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
     * Move o alvo: {@code right} unidades para a direita da tela (no plano
     * XZ, acompanhando o yaw) e {@code up} unidades para cima no eixo Y
     * do mundo. Valores negativos movem para a esquerda e para baixo.
     */
    public void pan(float right, float up) {
        // direita da tela: perpendicular à direção horizontal do olho
        // (sin(yaw), cos(yaw)) em (x, z), ou seja (cos(yaw), -sin(yaw))
        double radians = Math.toRadians(yaw);
        target.x += right * (float) Math.cos(radians);
        target.z -= right * (float) Math.sin(radians);
        target.y += up;
    }

    /** O yaw atual, em graus (ver {@link #rotate}). */
    public float yaw() {
        return yaw;
    }

    /**
     * Gira a câmera em volta do eixo Y do alvo: soma {@code degrees} ao yaw.
     * A altura do olho e a distância ao alvo não mudam, só o ângulo.
     */
    public void rotate(float degrees) {
        yaw = ((yaw + degrees) % 360f + 360f) % 360f;
    }

    /** Soma {@code delta} ao enquadramento: positivo afasta, negativo aproxima. */
    public void zoom(float delta) {
        size = Math.max(size + delta, MIN_SIZE);
    }

    /**
     * Direção unitária da superfície para a câmera. Em projeção ortográfica é
     * a mesma para toda a cena, mas muda com o yaw; serve de
     * {@code viewDirection} do brilho especular.
     */
    public Vector3f directionToEye() {
        return eyeOffset().normalize();
    }

    public Matrix4f viewMatrix() {
        Vector3f eye = eyeOffset().add(target);
        return new Matrix4f().lookAt(
                eye.x, eye.y, eye.z,
                target.x, target.y, target.z,
                0f, 1f, 0f);
    }

    /**
     * Vetor do alvo até o olho. Tudo o que depende de onde a câmera está
     * (viewMatrix, directionToEye e, por tabela, o picking) passa por aqui.
     */
    private Vector3f eyeOffset() {
        double radians = Math.toRadians(yaw);
        float horizontal = offset * SQRT_2;
        return new Vector3f(
                horizontal * (float) Math.sin(radians),
                offset,
                horizontal * (float) Math.cos(radians));
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
