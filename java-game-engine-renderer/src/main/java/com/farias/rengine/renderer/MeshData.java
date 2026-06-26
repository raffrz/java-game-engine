package com.farias.rengine.renderer;

public record MeshData(float[] vertices, float[] textures, float[] normals, int[] indices) {

    public static MeshData createCube() {
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

    public static MeshData createPlane() {
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
}