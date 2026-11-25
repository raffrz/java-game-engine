package com.farias.jengine.animation.ik;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.farias.jengine.animation.Pose;

/**
 * Gira uma junta (cabeça, olho, canhão de torre) para que um eixo dela
 * aponte para o alvo, limitado a um ângulo máximo a partir da pose animada.
 *
 * <p>
 * Trabalha no espaço do modelo e escreve a rotação local da junta.
 * Reaproveite a instância (tem scratch interno; não é thread-safe).
 */
public final class LookAtIk {

    private final IkJoints joints = new IkJoints();
    private final Vector3f position = new Vector3f();
    private final Vector3f forward = new Vector3f();
    private final Vector3f toTarget = new Vector3f();
    private final Quaternionf rotation = new Quaternionf();
    private final Quaternionf delta = new Quaternionf();

    /**
     * @param pose         a pose a corrigir (o espaço do modelo é
     *                     recalculado)
     * @param joint        a junta que gira
     * @param localForward o eixo da junta, no espaço dela, que deve apontar
     *                     para o alvo (por exemplo, 0,0,1 para a frente)
     * @param target       o alvo, no espaço do modelo
     * @param maxAngle     quanto, em radianos, a junta pode se afastar da
     *                     pose animada
     * @param weight       0 = não mexe, 1 = gira por inteiro
     */
    public void solve(Pose pose, int joint, Vector3fc localForward, Vector3fc target, float maxAngle, float weight) {
        pose.computeModelSpace();
        if (weight <= 0f || maxAngle <= 0f) {
            return;
        }
        joints.position(pose, joint, position);
        joints.worldRotation(pose, joint, rotation).transform(localForward, forward);
        toTarget.set(target).sub(position);
        if (forward.lengthSquared() < IkJoints.EPSILON || toTarget.lengthSquared() < IkJoints.EPSILON) {
            return;
        }
        delta.rotationTo(forward, toTarget);
        float angle = delta.angle();
        if (angle > Math.PI) {
            angle = (float) (2 * Math.PI) - angle;
        }
        float fraction = Math.min(weight, 1f);
        if (angle > maxAngle) {
            fraction *= maxAngle / angle;
        }
        joints.rotateInModelSpace(pose, joint, joints.weighted(delta, fraction));
        pose.computeModelSpace();
    }
}
