package com.farias.jengine.animation;

import java.util.Objects;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A animação de um canal (translação, rotação ou escala) de uma junta:
 * tempos dos keyframes, em segundos, e os valores neles.
 *
 * <p>
 * {@code values} tem 3 floats por keyframe para translação e escala e 4
 * ({@code x, y, z, w}) para rotação. Em {@link Interpolation#CUBICSPLINE},
 * cada keyframe tem 3 elementos seguidos (tangente de entrada, valor,
 * tangente de saída), como no glTF. Os arrays não são copiados: não os altere
 * depois de criar a trilha.
 *
 * @param joint         índice da junta no {@link Skeleton}
 * @param path          o canal animado
 * @param times         tempos crescentes, pelo menos um
 * @param values        valores por keyframe, ver acima
 * @param interpolation como ir de um keyframe ao próximo
 */
public record Track(int joint, Path path, float[] times, float[] values, Interpolation interpolation) {

    /** O canal da junta que a trilha anima. */
    public enum Path {
        TRANSLATION(3), ROTATION(4), SCALE(3);

        private final int components;

        Path(int components) {
            this.components = components;
        }

        /** Floats por valor: 3 (vetor) ou 4 (quaternion). */
        public int components() {
            return components;
        }
    }

    public Track {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(interpolation, "interpolation");
        if (joint < 0) {
            throw new IllegalArgumentException("joint must be >= 0: " + joint);
        }
        if (times.length == 0) {
            throw new IllegalArgumentException("track needs at least one keyframe");
        }
        for (int i = 1; i < times.length; i++) {
            if (!(times[i] > times[i - 1])) {
                throw new IllegalArgumentException("keyframe times must increase: times[" + i + "] = " + times[i]
                        + " after " + times[i - 1]);
            }
        }
        int perKeyframe = path.components() * (interpolation == Interpolation.CUBICSPLINE ? 3 : 1);
        if (values.length != times.length * perKeyframe) {
            throw new IllegalArgumentException("track has " + values.length + " values, expected "
                    + times.length * perKeyframe + " (" + times.length + " keyframes x " + perKeyframe + ")");
        }
    }

    /** O tempo do último keyframe. */
    public float endTime() {
        return times[times.length - 1];
    }

    /**
     * Escreve o valor do canal no instante {@code time} na junta da pose.
     * Antes do primeiro keyframe vale o primeiro; depois do último, o último.
     */
    public void apply(float time, Pose pose) {
        switch (path) {
            case TRANSLATION -> sampleVector(time, pose.translation(joint));
            case SCALE -> sampleVector(time, pose.scale(joint));
            case ROTATION -> sampleRotation(time, pose.rotation(joint), pose.scratchRotation);
        }
    }

    private void sampleVector(float time, Vector3f out) {
        int last = times.length - 1;
        if (time <= times[0] || last == 0) {
            readVector(valueOffset(0), out);
            return;
        }
        if (time >= times[last]) {
            readVector(valueOffset(last), out);
            return;
        }
        int k = keyframeBefore(time);
        float dt = times[k + 1] - times[k];
        float u = (time - times[k]) / dt;
        switch (interpolation) {
            case STEP -> readVector(valueOffset(k), out);
            case LINEAR -> {
                int a = valueOffset(k);
                int b = valueOffset(k + 1);
                out.set(lerp(values[a], values[b], u), lerp(values[a + 1], values[b + 1], u),
                        lerp(values[a + 2], values[b + 2], u));
            }
            case CUBICSPLINE -> out.set(hermite(k, 0, u, dt), hermite(k, 1, u, dt), hermite(k, 2, u, dt));
        }
    }

    private void sampleRotation(float time, Quaternionf out, Quaternionf next) {
        int last = times.length - 1;
        if (time <= times[0] || last == 0) {
            readRotation(valueOffset(0), out);
            return;
        }
        if (time >= times[last]) {
            readRotation(valueOffset(last), out);
            return;
        }
        int k = keyframeBefore(time);
        float dt = times[k + 1] - times[k];
        float u = (time - times[k]) / dt;
        switch (interpolation) {
            case STEP -> readRotation(valueOffset(k), out);
            case LINEAR -> {
                // slerp do JOML: já vai pelo caminho mais curto e cai para lerp com os dois quase alinhados
                readRotation(valueOffset(k), out);
                readRotation(valueOffset(k + 1), next);
                out.slerp(next, u).normalize();
            }
            case CUBICSPLINE -> out.set(hermite(k, 0, u, dt), hermite(k, 1, u, dt), hermite(k, 2, u, dt),
                    hermite(k, 3, u, dt)).normalize();
        }
    }

    /** O maior {@code k} com {@code times[k] <= time}; assume time dentro do intervalo. */
    private int keyframeBefore(float time) {
        int low = 0;
        int high = times.length - 1;
        while (high - low > 1) {
            int mid = (low + high) >>> 1;
            if (times[mid] <= time) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return low;
    }

    /** Onde começa o valor (não as tangentes) do keyframe {@code k} em {@code values}. */
    private int valueOffset(int k) {
        int c = path.components();
        return interpolation == Interpolation.CUBICSPLINE ? (k * 3 + 1) * c : k * c;
    }

    /**
     * Componente {@code i} da spline de Hermite entre os keyframes {@code k} e
     * {@code k + 1}, com as tangentes escaladas pela duração do intervalo
     * (definição do glTF).
     */
    private float hermite(int k, int i, float u, float dt) {
        int c = path.components();
        float p0 = values[(k * 3 + 1) * c + i];
        float m0 = values[(k * 3 + 2) * c + i] * dt;
        float p1 = values[((k + 1) * 3 + 1) * c + i];
        float m1 = values[((k + 1) * 3) * c + i] * dt;
        float u2 = u * u;
        float u3 = u2 * u;
        return (2 * u3 - 3 * u2 + 1) * p0 + (u3 - 2 * u2 + u) * m0 + (-2 * u3 + 3 * u2) * p1 + (u3 - u2) * m1;
    }

    private void readVector(int offset, Vector3f out) {
        out.set(values[offset], values[offset + 1], values[offset + 2]);
    }

    private void readRotation(int offset, Quaternionf out) {
        out.set(values[offset], values[offset + 1], values[offset + 2], values[offset + 3]).normalize();
    }

    private static float lerp(float a, float b, float u) {
        return a + (b - a) * u;
    }
}
