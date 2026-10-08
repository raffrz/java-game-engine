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
     * Um quad vertical (no plano XY, normal -Z) do tamanho dado, centrado na
     * origem - util para "planos" texturizados como pisos/tiles 3D (ex:
     * tabuleiro isometrico), rotacionando +90 graus em X pra deitar.
     */
    public static Mesh plane(Renderer renderer, float width, float height) {
        float[] vertices = {
                -width / 2, height / 2, 0,
                width / 2, height / 2, 0,
                width / 2, -height / 2, 0,
                -width / 2, -height / 2, 0,
        };
        float[] texCoords = { 0, 0, 1, 0, 1, 1, 0, 1 };
        // normal -Z: quem usa este plano como "chao" normalmente rotaciona
        // +90 graus em X pra deitar o quad (ver IsometricChess.drawBoard) -
        // essa rotacao leva -Z pra +Y, ou seja, o chao fica virado pra cima.
        // Com normal +Z ele ficaria virado pra baixo (escuro/invisivel).
        float[] normals = { 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1 };
        // winding invertido pra bater com a normal -Z (CCW visto de -Z) -
        // senao, com culling ligado, essa mesma rotacao de +90 deixa a face
        // "de frente" (CCW) virada pra -Y e o chao e culled visto de cima.
        int[] indices = { 0, 2, 3, 2, 0, 1 };

        return renderer.createMesh(new MeshData(vertices, texCoords, normals, indices));
    }
}
