package com.farias.rengine.renderer;

import org.joml.Vector3f;

/**
 * Caixa alinhada aos eixos (AABB) no espaço local de um modelo, isto é, antes
 * da matriz de modelo. Cada {@link Mesh} calcula a sua na criação, disponível
 * em {@link Mesh#bounds()}.
 *
 * <p>
 * Uma caixa vazia ({@link #empty()}) não contém nenhum ponto e nunca é
 * atingida por um raio.
 */
public record Bounds(Vector3f min, Vector3f max) {

    /**
     * A menor caixa que contém todos os pontos de {@code positions}, um array
     * plano x, y, z, x, y, z... (o formato de {@code MeshData.vertices()}).
     * Um array sem pontos resulta em {@link #empty()}.
     *
     * @throws IllegalArgumentException se {@code positions} for nulo ou seu
     *                                  tamanho não for múltiplo de 3
     */
    public static Bounds of(float[] positions) {
        if (positions == null || positions.length % 3 != 0) {
            throw new IllegalArgumentException("positions deve conter coordenadas XYZ completas.");
        }
        if (positions.length == 0) {
            return empty();
        }

        float minX = positions[0];
        float minY = positions[1];
        float minZ = positions[2];
        float maxX = minX;
        float maxY = minY;
        float maxZ = minZ;

        for (int i = 3; i < positions.length; i += 3) {
            minX = Math.min(minX, positions[i]);
            minY = Math.min(minY, positions[i + 1]);
            minZ = Math.min(minZ, positions[i + 2]);

            maxX = Math.max(maxX, positions[i]);
            maxY = Math.max(maxY, positions[i + 1]);
            maxZ = Math.max(maxZ, positions[i + 2]);
        }

        return new Bounds(new Vector3f(minX, minY, minZ), new Vector3f(maxX, maxY, maxZ));
    }

    /** Uma caixa sem pontos (mínimo acima do máximo em todos os eixos). */
    public static Bounds empty() {
        return new Bounds(
                new Vector3f(Float.POSITIVE_INFINITY),
                new Vector3f(Float.NEGATIVE_INFINITY));
    }

    public boolean isEmpty() {
        return min.x > max.x || min.y > max.y || min.z > max.z;
    }
}
