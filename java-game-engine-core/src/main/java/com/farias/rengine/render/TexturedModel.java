package com.farias.rengine.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class TexturedModel {
    private Vector3f position;
    private Vector3f rotation;
    private Vector3f scale;
    private Model model;
    private Texture texture;
    private Shader shader;

    private TexturedModel() {
        this.position = new Vector3f(0, 0, 0);
        this.rotation = new Vector3f();
        this.scale = new Vector3f(1, 1, 1);
    }

    public static TexturedModel load(String textureFileName, float[] vertices, float[] texCoords, int[] indices) {
        TexturedModel texturedModel = new TexturedModel();
        texturedModel.model = new Model(vertices, texCoords, indices);
        texturedModel.texture = new Texture("resources/textures/" + textureFileName + ".png");
        texturedModel.shader = new Shader("shader");

        return texturedModel;
    }

    public void draw(Matrix4f projection) {
        this.shader.bind();
        this.texture.bind(0);
        this.shader.setUniform("sampler", 0);
		this.shader.setUniform("projection", projection);
		this.model.draw();
    }

    public Matrix4f getModelMatrix() {
		Matrix4f translationMatrix = new Matrix4f().translation(position);
		Matrix4f rotationMatrix = new Matrix4f().rotationXYZ((float) Math.toRadians(-rotation.x), (float) Math.toRadians(-rotation.y), (float) Math.toRadians(-rotation.z));
		Matrix4f scalingMatrix = new Matrix4f().scale(scale);

		return translationMatrix.mul(scalingMatrix.mul(rotationMatrix));
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
