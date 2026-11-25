package com.farias.jengine.animation;

/**
 * Como uma {@link Track} vai de um keyframe ao próximo; os mesmos modos do
 * glTF 2.0.
 */
public enum Interpolation {
    /** Mantém o valor do keyframe anterior até o próximo. */
    STEP,
    /** Interpolação linear; rotações por slerp pelo caminho mais curto. */
    LINEAR,
    /**
     * Spline de Hermite com tangentes de entrada e saída por keyframe; os
     * valores vêm em trios (tangente de entrada, valor, tangente de saída).
     */
    CUBICSPLINE
}
