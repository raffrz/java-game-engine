package com.farias.rengine.engine3d;

import org.joml.Vector3f;

/**
 * Raio no espaço do mundo: os pontos {@code origin + t * direction} com
 * {@code t >= 0}. {@code direction} é normalizada, então {@code t} é a
 * distância, em unidades de mundo, a partir de {@code origin}.
 *
 * <p>
 * Criado por {@link Picking#ray}.
 */
public record Ray(Vector3f origin, Vector3f direction) {

    /** O ponto do raio a distância {@code t} da origem. */
    public Vector3f pointAt(float t) {
        return new Vector3f(
                Math.fma(direction.x, t, origin.x),
                Math.fma(direction.y, t, origin.y),
                Math.fma(direction.z, t, origin.z));
    }
}
