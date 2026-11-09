package com.farias.jengine.renderer;

import org.joml.Matrix4f;

/**
 * Um desenho: um {@link Mesh} com um {@link Material} e a matriz de modelo.
 *
 * <p>
 * {@code jointPalette} é opcional ({@code null} em malhas sem skin): as
 * matrizes de skinning de cada junta, column-major, 16 floats por junta. O
 * {@link Renderer} as passa no uniform {@value #JOINTS_UNIFORM} a cada
 * comando, então cada instância animada tem a sua pose mesmo compartilhando
 * material. O array não é copiado: não o altere antes do {@code endFrame}.
 */
public record RenderCommand(Mesh mesh, Material material, Matrix4f modelMatrix, float[] jointPalette) {

    /** Nome do uniform {@code mat4[]} que recebe {@link #jointPalette()}. */
    public static final String JOINTS_UNIFORM = "uJoints";

    public RenderCommand {
        if (jointPalette != null && jointPalette.length % 16 != 0) {
            throw new IllegalArgumentException(
                    "jointPalette must have 16 floats per joint, got " + jointPalette.length);
        }
    }

    /** Um desenho sem skin. */
    public RenderCommand(Mesh mesh, Material material, Matrix4f modelMatrix) {
        this(mesh, material, modelMatrix, null);
    }

    public void setModelMatrix(Matrix4f newMatrix) {
        this.modelMatrix.set(newMatrix);
    }
}
