package com.farias.jengine.toolkit3d;

import com.farias.jengine.renderer.MeshData;

/**
 * Geometria das formas basicas, como dados. Funcoes puras (sem GPU): servem
 * a quem cria o Mesh direto no Renderer; {@link Shapes} faz isso por voce.
 */
public final class ShapeData {

    private ShapeData() {
    }

    /** Cubo de lado 1 centrado na origem, com normais por face. */
    public static MeshData cube() {
        float[] vertices = {
                // Frente (+Z)
                -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
                // Trás (-Z)
                -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -0.5f,
                // Topo (+Y)
                -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f,
                // Base (-Y)
                -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f,
                // Direita (+X)
                0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f,
                // Esquerda (-X)
                -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, -0.5f
        };

        float[] textures = {
                // Frente
                0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                // Trás
                1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                // Topo
                0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f,
                // Base
                1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
                // Direita
                1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                // Esquerda
                0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f
        };

        float[] normals = {
                // Frente (+Z)
                0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
                // Trás (-Z)
                0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f,
                // Topo (+Y)
                0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
                // Base (-Y)
                0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
                // Direita (+X)
                1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                // Esquerda (-X)
                -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f
        };

        int[] indices = {
                0, 1, 2, 0, 2, 3, // Frente
                4, 5, 6, 4, 6, 7, // Trás
                8, 9, 10, 8, 10, 11, // Topo
                12, 13, 14, 12, 14, 15, // Base
                16, 17, 18, 16, 18, 19, // Direita
                20, 21, 22, 20, 22, 23 // Esquerda
        };

        return new MeshData(vertices, textures, normals, indices);
    }

    /** Quad horizontal de lado 1 (plano XZ, normal +Y), centrado na origem. */
    public static MeshData floor() {
        // Um plano simples composto por 4 vértices formando um quadrado no plano XZ
        float[] vertices = {
                -0.5f, 0.0f, 0.5f, // 0: Inferior Esquerdo (Frente)
                0.5f, 0.0f, 0.5f, // 1: Inferior Direito (Frente)
                0.5f, 0.0f, -0.5f, // 2: Superior Direito (Trás)
                -0.5f, 0.0f, -0.5f // 3: Superior Esquerdo (Trás)
        };

        float[] textures = {
                0.0f, 0.0f, // 0
                1.0f, 0.0f, // 1
                1.0f, 1.0f, // 2
                0.0f, 1.0f // 3
        };

        // Como o chão está plano, todas as normais apontam rigidamente para cima (+Y)
        float[] normals = {
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f
        };

        // Dois triângulos compartilhando vértices em ordem anti-horária
        // (Counter-Clockwise)
        int[] indices = {
                0, 1, 2,
                0, 2, 3
        };

        return new MeshData(vertices, textures, normals, indices);
    }

    /**
     * Quad vertical (plano XY, normal -Z) do tamanho dado, centrado na origem.
     * Rotacionado +90 graus em X ele deita virado para cima (normal +Y), o que
     * serve de piso ou tile.
     */
    public static MeshData plane(float width, float height) {
        float[] vertices = {
                -width / 2, height / 2, 0,
                width / 2, height / 2, 0,
                width / 2, -height / 2, 0,
                -width / 2, -height / 2, 0,
        };
        float[] texCoords = { 0, 0, 1, 0, 1, 1, 0, 1 };
        float[] normals = { 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1 };
        // winding CCW visto de -Z, para bater com a normal: com culling
        // ligado, e essa a face que sobra depois de deitar o quad.
        int[] indices = { 0, 2, 3, 2, 0, 1 };

        return new MeshData(vertices, texCoords, normals, indices);
    }
}
