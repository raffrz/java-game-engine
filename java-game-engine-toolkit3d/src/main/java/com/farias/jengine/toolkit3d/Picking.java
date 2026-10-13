package com.farias.jengine.toolkit3d;

import org.joml.Intersectionf;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.farias.jengine.renderer.Bounds;
import com.farias.jengine.renderer.RenderView;

/**
 * Picking por raycasting: descobre o que está sob o mouse numa cena 3D.
 * Funções puras, sem GPU, janela ou serviço, no mesmo estilo de
 * {@link Projections}.
 *
 * <p>
 * Uso típico:
 *
 * <pre>{@code
 * Vector2f ndc = Picking.toNdc(input.getMouseX(), input.getMouseY(), window.getWidth(), window.getHeight());
 * Ray ray = Picking.ray(view3D, ndc);
 * float t = Picking.rayBounds(ray, model.getMesh().bounds(), model.getModelMatrix());
 * if (!Float.isNaN(t)) {
 *     Vector3f hit = ray.pointAt(t);
 * }
 * }</pre>
 *
 * <p>
 * As funções de distância devolvem {@link Float#NaN} quando não há acerto. O
 * {@link RenderView} e a matriz de modelo devem ser os mesmos usados para
 * desenhar, senão o raio e o que aparece na tela deixam de coincidir.
 */
public final class Picking {

    private Picking() {
    }

    /**
     * Converte a posição do mouse para coordenadas normalizadas (NDC): origem no
     * centro, X para a direita e Y para cima, ambos em [-1, 1].
     *
     * @param mouseX       pixels lógicos da janela, com origem no canto superior
     *                     esquerdo e Y para baixo
     * @param windowWidth  largura lógica da janela ({@code Window.getWidth()}),
     *                     não a do framebuffer
     * @param windowHeight altura lógica da janela
     * @throws IllegalArgumentException se alguma dimensão não for positiva
     */
    public static Vector2f toNdc(double mouseX, double mouseY, int windowWidth, int windowHeight) {
        if (windowWidth <= 0 || windowHeight <= 0) {
            throw new IllegalArgumentException("Window dimensions must be positive.");
        }

        float x = (float) (2.0 * mouseX / windowWidth - 1.0);
        float y = (float) (1.0 - 2.0 * mouseY / windowHeight);
        return new Vector2f(x, y);
    }

    /**
     * O raio, no mundo, que passa pelo ponto NDC dado. Vale para projeção em
     * perspectiva (raios saindo do olho) e ortográfica (raios paralelos).
     *
     * @param view o mesmo {@link RenderView} usado para desenhar a cena
     * @throws IllegalStateException se as matrizes da view não permitirem
     *                               construir um raio válido
     */
    public static Ray ray(RenderView view, Vector2f ndc) {
        Matrix4f inverseViewProjection = new Matrix4f(view.projectionMatrix())
                .mul(view.viewMatrix())
                .invert();

        Vector4f nearClip = new Vector4f(ndc.x, ndc.y, -1f, 1f).mul(inverseViewProjection);
        Vector4f farClip = new Vector4f(ndc.x, ndc.y, 1f, 1f).mul(inverseViewProjection);

        if (Math.abs(nearClip.w) < 1e-7f || Math.abs(farClip.w) < 1e-7f) {
            throw new IllegalStateException("Não foi possível fazer a divisão por W.");
        }

        Vector3f nearWorld = new Vector3f(nearClip.x / nearClip.w, nearClip.y / nearClip.w, nearClip.z / nearClip.w);
        Vector3f farWorld = new Vector3f(farClip.x / farClip.w, farClip.y / farClip.w, farClip.z / farClip.w);

        Vector3f direction = new Vector3f(farWorld).sub(nearWorld);
        if (direction.lengthSquared() < 1e-12f) {
            throw new IllegalStateException("A direção do raio é inválida.");
        }

        return new Ray(nearWorld, direction.normalize());
    }

    /**
     * Distância ao longo do raio até a caixa {@code localBounds} de um modelo
     * posicionado por {@code modelMatrix}; considera translação, rotação e
     * escala do modelo.
     *
     * @return a distância até a entrada na caixa; {@code 0} se a origem do raio
     *         está dentro dela; {@link Float#NaN} se a caixa está fora do raio,
     *         atrás da origem ou vazia
     */
    public static float rayBounds(Ray ray, Bounds localBounds, Matrix4f modelMatrix) {
        if (localBounds.isEmpty()) {
            return Float.NaN;
        }

        Matrix4f inverseModel = new Matrix4f(modelMatrix).invert();
        Vector3f localOrigin = inverseModel.transformPosition(new Vector3f(ray.origin()));
        // Sem normalizar: a transformação é linear, então t continua sendo o do raio no mundo.
        Vector3f localDirection = inverseModel.transformDirection(new Vector3f(ray.direction()));

        Vector2f interval = new Vector2f();
        boolean hit = Intersectionf.intersectRayAab(
                localOrigin, localDirection, localBounds.min(), localBounds.max(), interval);
        if (!hit) {
            return Float.NaN;
        }

        float tEnter = interval.x;
        float tExit = interval.y;
        if (tExit < 0f) {
            return Float.NaN;
        }
        return Math.max(tEnter, 0f);
    }

    /**
     * Distância ao longo do raio até o plano horizontal {@code y = planeY}.
     * Combine com {@link Ray#pointAt} para obter o ponto de acerto.
     *
     * @return a distância, ou {@link Float#NaN} se o raio é paralelo ao plano
     *         ou o plano está atrás da origem
     */
    public static float rayPlaneY(Ray ray, float planeY) {
        float directionY = ray.direction().y;
        if (Math.abs(directionY) < 1e-7f) {
            return Float.NaN;
        }

        float t = (planeY - ray.origin().y) / directionY;
        return t < 0f ? Float.NaN : t;
    }
}
