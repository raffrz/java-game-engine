package com.farias.rengine.toolkit2d.legacy;

import org.joml.Matrix4f;

/**
 * Projecao ortografica 2D "tela". Funcao pura - cada chamada devolve uma
 * matriz nova. Legado: sera substituido pelo Canvas (ver plano).
 */
public final class Projections2D {

    private Projections2D() {
    }

    /**
     * Ortografica 2D "tela": origem (0,0) no canto superior esquerdo, Y
     * crescendo para baixo, em pixels logicos (width x height).
     */
    public static Matrix4f orthographic2D(float width, float height) {
        return new Matrix4f().setOrtho(0, width, -height, 0, 1000, -1000);
    }
}
