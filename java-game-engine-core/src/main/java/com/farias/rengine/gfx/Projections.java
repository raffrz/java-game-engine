package com.farias.rengine.gfx;

import org.joml.Matrix4f;

/**
 * Matrizes de projecao prontas para os modos mais comuns. Funcoes puras -
 * cada chamada devolve uma matriz nova.
 */
public final class Projections {

    private Projections() {
    }

    /**
     * Ortografica 2D "tela": origem (0,0) no canto superior esquerdo, Y
     * crescendo para baixo, em pixels logicos (width x height).
     */
    public static Matrix4f orthographic2D(float width, float height) {
        return new Matrix4f().setOrtho(0, width, -height, 0, 1000, -1000);
    }

    /**
     * Ortografica "3D" (sem perspectiva, para visao isometrica): meia-altura
     * `size`, ajustada pelo aspect ratio.
     */
    public static Matrix4f orthographic3D(float size, float aspect) {
        return new Matrix4f().setOrtho(-size * aspect, size * aspect, -size, size, -1000f, 1000f);
    }

    private static final float DEFAULT_FOV_DEGREES = 70f;
    private static final float DEFAULT_Z_NEAR = 1.0f;
    private static final float DEFAULT_Z_FAR = 1000f;

    public static Matrix4f perspective(int width, int height) {
        float aspectRatio = (float) width / (float) height;
        return new Matrix4f().setPerspective((float) Math.toRadians(DEFAULT_FOV_DEGREES), aspectRatio, DEFAULT_Z_NEAR,
                DEFAULT_Z_FAR);
    }
}
