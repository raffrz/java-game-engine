package com.farias.rengine.gfx;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.farias.rengine.renderer.Material;
import com.farias.rengine.renderer.Mesh;

/**
 * Um Mesh + Material (tipicamente lit via {@link Materials#lit}) mais
 * posicao/rotacao/escala no mundo.
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
                (float) Math.toRadians(-rotation.x),
                (float) Math.toRadians(-rotation.y),
                (float) Math.toRadians(-rotation.z));
        Matrix4f scalingMatrix = new Matrix4f().scale(scale);

        return translationMatrix.mul(scalingMatrix.mul(rotationMatrix));
    }

    public Mesh getMesh() {
        return mesh;
    }

    public Material getMaterial() {
        return material;
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
