package com.farias.jengine.renderer;

/**
 * Os dados de uma malha antes de ir para a GPU: posições (x, y, z), UVs
 * (u, v), normais (x, y, z), índices de triângulo e, opcionalmente, a skin.
 *
 * <p>
 * Skin (malha deformada por esqueleto): {@code joints} tem 4 índices de junta
 * por vértice e {@code weights} os 4 pesos correspondentes. Os dois vêm juntos,
 * ou nenhum ({@code null}, malha estática). O {@link Mesh} os sobe como
 * atributos 3 ({@code ivec4}) e 4 ({@code vec4}).
 */
public record MeshData(float[] vertices, float[] textures, float[] normals, int[] indices, int[] joints,
        float[] weights) {

    /** Influências de junta por vértice numa malha com skin. */
    public static final int INFLUENCES_PER_VERTEX = 4;

    public MeshData {
        if ((joints == null) != (weights == null)) {
            throw new IllegalArgumentException("joints and weights must be given together");
        }
        if (joints != null) {
            int vertexCount = vertices.length / 3;
            int expected = vertexCount * INFLUENCES_PER_VERTEX;
            if (joints.length != expected || weights.length != expected) {
                throw new IllegalArgumentException("skin needs " + INFLUENCES_PER_VERTEX
                        + " joints and weights per vertex (" + expected + " for " + vertexCount + " vertices), got "
                        + joints.length + " joints and " + weights.length + " weights");
            }
        }
    }

    /** Uma malha estática, sem skin. */
    public MeshData(float[] vertices, float[] textures, float[] normals, int[] indices) {
        this(vertices, textures, normals, indices, null, null);
    }

    /** Se a malha tem skin (juntas e pesos por vértice). */
    public boolean skinned() {
        return joints != null;
    }
}
