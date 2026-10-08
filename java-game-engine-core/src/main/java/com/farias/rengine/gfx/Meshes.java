package com.farias.rengine.gfx;

import com.farias.rengine.renderer.Mesh;
import com.farias.rengine.renderer.MeshData;
import com.farias.rengine.renderer.Renderer;

/**
 * Fabrica de primitivas de Mesh simples.
 */
public final class Meshes {

    private Meshes() {
    }

    /**
     * Um quad vertical (no plano XY, normal +Z) do tamanho dado, centrado na
     * origem - util para "planos" texturizados como pisos/tiles 3D vistos de
     * frente (ex: tabuleiro isometrico).
     */
    public static Mesh plane(Renderer renderer, float width, float height) {
        float[] vertices = {
                -width / 2, height / 2, 0,
                width / 2, height / 2, 0,
                width / 2, -height / 2, 0,
                -width / 2, -height / 2, 0,
        };
        float[] texCoords = { 0, 0, 1, 0, 1, 1, 0, 1 };
        float[] normals = { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1 };
        int[] indices = { 0, 3, 2, 2, 1, 0 };

        return renderer.createMesh(new MeshData(vertices, texCoords, normals, indices));
    }
}
