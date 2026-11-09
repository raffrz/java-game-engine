package com.farias.jengine.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Esqueletos pequenos para os testes. */
final class TestSkeletons {

    private TestSkeletons() {
    }

    /** Cadeia raiz -> meio -> ponta ao longo de +Y, ossos de 1 unidade, amarrada no repouso. */
    static Skeleton chain() {
        return Skeleton.ofRestPose(
                new String[] { "root", "mid", "tip" },
                new int[] { -1, 0, 1 },
                new Vector3f[] { new Vector3f(0, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 1, 0) },
                new Quaternionf[] { new Quaternionf(), new Quaternionf(), new Quaternionf() },
                new Vector3f[] { new Vector3f(1), new Vector3f(1), new Vector3f(1) });
    }
}
