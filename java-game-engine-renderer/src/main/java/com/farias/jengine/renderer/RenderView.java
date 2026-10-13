package com.farias.jengine.renderer;

import org.joml.Matrix4f;

public class RenderView {

    private int width;
    private int height;
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;

    public RenderView(int width, int height, Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        this.width = width;
        this.height = height;
        this.projectionMatrix = projectionMatrix;
        this.viewMatrix = viewMatrix;
    }

    public Matrix4f projectionMatrix() {
        return projectionMatrix;
    }

    public Matrix4f viewMatrix() {
        return viewMatrix;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
