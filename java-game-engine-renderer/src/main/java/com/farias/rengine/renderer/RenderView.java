package com.farias.rengine.renderer;

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

    public static RenderView createDefault(int width, int height) {
        // Matrix4f view = new Matrix4f()
        //         .lookAt(
        //                 0, 0, 0,    // eye: Posição da câmera (afastada em Z positivo)
        //                 0, 0, 0,    // center: Ponto para onde ela olha (origem, onde o cubo orbita)
        //                 0, 1, 0);   // up: Vetor "para cima" da câmera

        Matrix4f view = new Matrix4f()
                .rotate((float) Math.toRadians(0), 1, 0, 0)
                .rotate((float) Math.toRadians(0), 0, 1, 0)
                .rotate((float) Math.toRadians(0), 0, 0, 1)
                .translate(0, 0, 0);
                        
        Matrix4f projection = new Matrix4f()
                .perspective(
                        (float) Math.toRadians(70),
                        (float) width / height, 
                        1.0f,
                        1000.0f);
                        
        return new RenderView(width, height, projection, view);
    }

}
