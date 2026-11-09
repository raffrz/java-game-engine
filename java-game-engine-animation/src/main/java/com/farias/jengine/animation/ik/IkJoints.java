package com.farias.jengine.animation.ik;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

import com.farias.jengine.animation.Pose;

/**
 * Operações sobre juntas no espaço do modelo, comuns aos solvers. Cada solver
 * tem a sua instância (scratch reaproveitado, sem alocar por chamada).
 */
final class IkJoints {

    static final float EPSILON = 1e-5f;

    private final Quaternionf world = new Quaternionf();
    private final Quaternionf change = new Quaternionf();

    /** Posição da junta no espaço do modelo (a do último computeModelSpace). */
    Vector3f position(Pose pose, int joint, Vector3f out) {
        return pose.modelSpace(joint).getTranslation(out);
    }

    /** Rotação da junta no espaço do modelo, sem a escala. */
    Quaternionf worldRotation(Pose pose, int joint, Quaternionf out) {
        return pose.modelSpace(joint).getNormalizedRotation(out);
    }

    /**
     * Gira a junta por {@code delta}, uma rotação no espaço do modelo, em
     * torno dela mesma: a nova rotação de mundo é {@code delta * atual}. Como
     * a pose guarda rotações locais, vira {@code local * (atual⁻¹ * delta *
     * atual)}. Não recalcula o espaço do modelo.
     */
    void rotateInModelSpace(Pose pose, int joint, Quaternionfc delta) {
        worldRotation(pose, joint, world);
        world.conjugate(change).mul(delta).mul(world);
        pose.rotation(joint).mul(change).normalize();
    }

    /** Gira por {@code angle} radianos em torno de {@code axis} (unitário, espaço do modelo). */
    void rotateInModelSpace(Pose pose, int joint, Vector3f axis, float angle, Quaternionf scratch) {
        rotateInModelSpace(pose, joint, scratch.fromAxisAngleRad(axis, angle));
    }

    /** Se {@code descendant} está abaixo de {@code ancestor} na hierarquia. */
    static boolean isBelow(Pose pose, int descendant, int ancestor) {
        for (int j = pose.skeleton().parent(descendant); j >= 0; j = pose.skeleton().parent(j)) {
            if (j == ancestor) {
                return true;
            }
        }
        return false;
    }

    static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Ângulo entre dois vetores não nulos, em radianos. */
    static float angle(Vector3f a, Vector3f b) {
        float lengths = a.length() * b.length();
        if (lengths < EPSILON) {
            return 0f;
        }
        return (float) Math.acos(clamp(a.dot(b) / lengths, -1f, 1f));
    }
}
