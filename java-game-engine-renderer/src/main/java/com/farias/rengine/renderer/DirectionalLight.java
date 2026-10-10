package com.farias.rengine.renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Luz direcional com volume ortografico para shadow mapping. Deriva as
 * matrizes de view/projecao/light-space a partir da direcao (do alvo para a
 * luz), do ponto focal, da distancia e da extensao da area coberta.
 */
public final class DirectionalLight {
    private final Vector3f direction;
    private final Matrix4f view;
    private final Matrix4f projection;
    private final Matrix4f lightSpaceMatrix;

    public DirectionalLight(Vector3f direction, Vector3f focus, float distance, float extent, float near, float far) {
        this.direction = new Vector3f(direction).normalize();
        this.view = new Matrix4f().lookAt(
                new Vector3f(this.direction).mul(distance).add(focus),
                new Vector3f(focus),
                new Vector3f(0.0f, 1.0f, 0.0f));
        this.projection = new Matrix4f().ortho(-extent, extent, -extent, extent, near, far);
        this.lightSpaceMatrix = new Matrix4f(projection).mul(view);
    }

    public Vector3f direction() {
        return new Vector3f(direction);
    }

    public Matrix4f lightSpaceMatrix() {
        return new Matrix4f(lightSpaceMatrix);
    }

    public RenderView shadowView(Texture shadowMap) {
        return new RenderView(shadowMap.getWidth(), shadowMap.getHeight(), projection, view);
    }
}
