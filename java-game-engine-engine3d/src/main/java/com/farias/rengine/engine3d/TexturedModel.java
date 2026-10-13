package com.farias.rengine.engine3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.farias.rengine.renderer.Material;
import com.farias.rengine.renderer.Mesh;
import com.farias.rengine.renderer.RenderCommand;

/**
 * Um {@link Mesh} e um {@link Material} (tipicamente lit, via
 * {@link Materials#lit}) com posição, rotação e escala no mundo.
 *
 * <p>
 * A rotação é em graus, ordem XYZ, no sentido positivo. {@link #getModelMatrix()}
 * é a fonte única da matriz de modelo: use a mesma para desenhar e para o
 * {@link Picking}.
 */
public final class TexturedModel {
    private final Mesh mesh;
    private final Material material;
    private final Vector3f position = new Vector3f(0, 0, 0);
    private final Vector3f rotation = new Vector3f();
    private final Vector3f scale = new Vector3f(1, 1, 1);

    public TexturedModel(Mesh mesh, Material material) {
        this.mesh = mesh;
        this.material = material;
    }

    public Matrix4f getModelMatrix() {
        Matrix4f translationMatrix = new Matrix4f().translation(position);
        Matrix4f rotationMatrix = new Matrix4f().rotationXYZ(
                (float) Math.toRadians(rotation.x),
                (float) Math.toRadians(rotation.y),
                (float) Math.toRadians(rotation.z));
        Matrix4f scalingMatrix = new Matrix4f().scale(scale);

        // T * R * S: escala no espaço do modelo, antes de girar. Na ordem
        // contrária, uma escala não uniforme deformaria o modelo girado.
        return translationMatrix.mul(rotationMatrix.mul(scalingMatrix));
    }

    /**
     * O comando que desenha este modelo com a transformação atual. A matriz é
     * nova a cada chamada, então o comando pode ser guardado até o
     * {@code endFrame}.
     */
    public RenderCommand command() {
        return new RenderCommand(mesh, material, getModelMatrix());
    }

    public Mesh getMesh() {
        return mesh;
    }

    public Material getMaterial() {
        return material;
    }

    /**
     * O material como {@link LitMaterial}, para refiná-lo (luz, brilho,
     * sombra). Vale para modelos criados com o material padrão da engine, como
     * os de {@link ModelLoader#loadWavefrontOBJ(String, boolean)}.
     *
     * @throws IllegalStateException se o modelo usa outro tipo de material
     */
    public LitMaterial lit() {
        if (material instanceof LitMaterial lit) {
            return lit;
        }
        throw new IllegalStateException("O material do modelo não é um LitMaterial: " + material.getClass().getName());
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public Vector3f getScale() {
        return scale;
    }
}
