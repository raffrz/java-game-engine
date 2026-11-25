package com.farias.jengine.toolkit3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Posição, rotação e escala de um objeto no mundo, e a matriz de modelo que
 * sai delas. Comum a {@link TexturedModel} e {@link SkinnedModel}, então os
 * dois seguem a mesma convenção: rotação em graus, ordem XYZ, sentido
 * positivo, e matriz T * R * S (escala no espaço do modelo, antes de girar;
 * na ordem contrária, uma escala não uniforme deformaria o modelo girado).
 *
 * <p>
 * Os vetores são mutáveis: mexa neles direto
 * ({@code transform.position().set(1, 0, 2)}).
 */
public final class Transform {

    private final Vector3f position = new Vector3f();
    private final Vector3f rotation = new Vector3f();
    private final Vector3f scale = new Vector3f(1, 1, 1);

    public Vector3f position() {
        return position;
    }

    /** Em graus, por eixo. */
    public Vector3f rotation() {
        return rotation;
    }

    public Vector3f scale() {
        return scale;
    }

    /**
     * A matriz de modelo atual, numa matriz nova (pode ser guardada num
     * {@code RenderCommand} até o {@code endFrame}).
     */
    public Matrix4f matrix() {
        return matrix(new Matrix4f());
    }

    /** A matriz de modelo atual, escrita em {@code out}. */
    public Matrix4f matrix(Matrix4f out) {
        return out.translation(position)
                .rotateXYZ((float) Math.toRadians(rotation.x), (float) Math.toRadians(rotation.y),
                        (float) Math.toRadians(rotation.z))
                .scale(scale);
    }
}
