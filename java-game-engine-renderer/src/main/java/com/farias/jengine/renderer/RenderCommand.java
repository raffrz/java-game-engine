package com.farias.jengine.renderer;

import org.joml.Matrix4f;

public record RenderCommand(Mesh mesh, Material material, Matrix4f modelMatrix) {
     public void setModelMatrix(Matrix4f newMatrix) {
        this.modelMatrix.set(newMatrix); 
    }
}
