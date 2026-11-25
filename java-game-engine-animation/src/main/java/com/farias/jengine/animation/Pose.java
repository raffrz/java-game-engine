package com.farias.jengine.animation;

import java.util.Objects;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Uma pose de um {@link Skeleton}: translação, rotação e escala locais
 * (relativas ao pai) de cada junta, mutáveis. Uma por instância animada; os
 * clips ({@link AnimationClip#sample}), o blend ({@link Poses#blend}) e o IK
 * escrevem nela.
 *
 * <p>
 * Não aloca depois de criada: as matrizes do espaço do modelo e o scratch do
 * palette são reaproveitados.
 */
public final class Pose {

    /** Floats por junta no palette de skinning: uma {@code mat4} column-major. */
    public static final int FLOATS_PER_JOINT = 16;

    private final Skeleton skeleton;
    private final Vector3f[] translations;
    private final Quaternionf[] rotations;
    private final Vector3f[] scales;
    private final Matrix4f[] modelSpace;
    private final Matrix4f scratch = new Matrix4f();
    /** Scratch para quem escreve rotações na pose (o slerp das trilhas), sem alocar por amostra. */
    final Quaternionf scratchRotation = new Quaternionf();

    /** Uma pose no repouso de {@code skeleton}. */
    public Pose(Skeleton skeleton) {
        this.skeleton = Objects.requireNonNull(skeleton, "skeleton");
        int count = skeleton.jointCount();
        translations = new Vector3f[count];
        rotations = new Quaternionf[count];
        scales = new Vector3f[count];
        modelSpace = new Matrix4f[count];
        for (int j = 0; j < count; j++) {
            translations[j] = new Vector3f();
            rotations[j] = new Quaternionf();
            scales[j] = new Vector3f();
            modelSpace[j] = new Matrix4f();
        }
        setToRest();
    }

    public Skeleton skeleton() {
        return skeleton;
    }

    public int jointCount() {
        return translations.length;
    }

    /** Translação local da junta; mutável. */
    public Vector3f translation(int joint) {
        return translations[joint];
    }

    /** Rotação local da junta; mutável (mantenha-a normalizada). */
    public Quaternionf rotation(int joint) {
        return rotations[joint];
    }

    /** Escala local da junta; mutável. */
    public Vector3f scale(int joint) {
        return scales[joint];
    }

    /** Volta todas as juntas para a pose de repouso do esqueleto. */
    public Pose setToRest() {
        for (int j = 0; j < translations.length; j++) {
            translations[j].set(skeleton.restTranslation(j));
            rotations[j].set(skeleton.restRotation(j));
            scales[j].set(skeleton.restScale(j));
        }
        return this;
    }

    /** Copia {@code other}, que precisa ser do mesmo esqueleto. */
    public Pose set(Pose other) {
        requireSameSkeleton(other);
        for (int j = 0; j < translations.length; j++) {
            translations[j].set(other.translations[j]);
            rotations[j].set(other.rotations[j]);
            scales[j].set(other.scales[j]);
        }
        return this;
    }

    /**
     * Recalcula a matriz de cada junta no espaço do modelo
     * ({@code modelo[pai] * T * R * S}). Chame depois de mexer na pose e antes
     * de ler {@link #modelSpace}.
     */
    public Pose computeModelSpace() {
        for (int j = 0; j < translations.length; j++) {
            Matrix4f model = modelSpace[j].translationRotateScale(translations[j], rotations[j], scales[j]);
            int parent = skeleton.parent(j);
            if (parent >= 0) {
                modelSpace[parent].mul(model, model);
            }
        }
        return this;
    }

    /**
     * A matriz da junta no espaço do modelo, como deixada pelo último
     * {@link #computeModelSpace()} (ou pelo último {@link #skinningPalette}).
     */
    public Matrix4fc modelSpace(int joint) {
        return modelSpace[joint];
    }

    /**
     * Recalcula o espaço do modelo e escreve em {@code out} a matriz de
     * skinning de cada junta ({@code modelo[j] * inversaDeBind[j]}),
     * column-major, {@link #FLOATS_PER_JOINT} floats por junta. É o que o
     * vertex shader recebe para deformar a malha.
     *
     * @param out pelo menos {@code FLOATS_PER_JOINT * jointCount()} floats
     */
    public void skinningPalette(float[] out) {
        int needed = FLOATS_PER_JOINT * translations.length;
        if (out.length < needed) {
            throw new IllegalArgumentException("palette has " + out.length + " floats, needs " + needed);
        }
        computeModelSpace();
        for (int j = 0; j < translations.length; j++) {
            modelSpace[j].mul(skeleton.inverseBind(j), scratch).get(out, j * FLOATS_PER_JOINT);
        }
    }

    void requireSameSkeleton(Pose other) {
        if (other.skeleton != skeleton) {
            throw new IllegalArgumentException("poses belong to different skeletons");
        }
    }
}
