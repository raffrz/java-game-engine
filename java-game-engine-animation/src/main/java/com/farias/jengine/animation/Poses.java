package com.farias.jengine.animation;

/** Operações puras entre poses. */
public final class Poses {

    private Poses() {
    }

    /**
     * Mistura {@code a} e {@code b} junta por junta: translação e escala
     * lineares, rotação por slerp pelo caminho mais curto. {@code weight = 0}
     * dá {@code a}, {@code 1} dá {@code b}. {@code out} pode ser {@code a} ou
     * {@code b}. As três poses precisam ser do mesmo esqueleto.
     */
    public static void blend(Pose a, Pose b, float weight, Pose out) {
        a.requireSameSkeleton(b);
        a.requireSameSkeleton(out);
        for (int j = 0; j < a.jointCount(); j++) {
            a.translation(j).lerp(b.translation(j), weight, out.translation(j));
            a.scale(j).lerp(b.scale(j), weight, out.scale(j));
            a.rotation(j).slerp(b.rotation(j), weight, out.rotation(j)).normalize();
        }
    }
}
