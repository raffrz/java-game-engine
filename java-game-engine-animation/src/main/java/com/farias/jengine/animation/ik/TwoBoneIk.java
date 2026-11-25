package com.farias.jengine.animation.ik;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.farias.jengine.animation.Pose;

/**
 * IK analítico de dois ossos (braço: ombro, cotovelo, mão; perna: quadril,
 * joelho, pé). Pela lei dos cossenos acha os ângulos que levam a ponta ao
 * alvo, e o {@code pole} escolhe para que lado a junta do meio dobra.
 *
 * <p>
 * Trabalha no espaço do modelo e escreve rotações locais na pose (da raiz e
 * do meio). Alvo fora de alcance: estica na direção dele. Reaproveite a
 * instância (tem scratch interno; não é thread-safe).
 *
 * <pre>{@code
 * TwoBoneIk arm = new TwoBoneIk();
 * arm.solve(pose, shoulder, elbow, hand, model.toModelSpace(target, t), elbowHint, 1f);
 * }</pre>
 */
public final class TwoBoneIk {

    private final IkJoints joints = new IkJoints();
    private final Vector3f a = new Vector3f();
    private final Vector3f b = new Vector3f();
    private final Vector3f c = new Vector3f();
    private final Vector3f t = new Vector3f();
    private final Vector3f ab = new Vector3f();
    private final Vector3f ac = new Vector3f();
    private final Vector3f at = new Vector3f();
    private final Vector3f cb = new Vector3f();
    private final Vector3f ba = new Vector3f();
    private final Vector3f axis = new Vector3f();
    private final Vector3f u = new Vector3f();
    private final Vector3f v = new Vector3f();
    private final Vector3f side = new Vector3f();
    private final Quaternionf scratch = new Quaternionf();

    /**
     * Leva {@code end} até {@code target} girando {@code root} e {@code mid}.
     *
     * @param pose   a pose a corrigir (o espaço do modelo é recalculado)
     * @param root   junta da raiz (ombro, quadril)
     * @param mid    junta do meio (cotovelo, joelho), abaixo de {@code root}
     * @param end    junta da ponta (mão, pé), abaixo de {@code mid}
     * @param target alvo da ponta, no espaço do modelo
     * @param pole   ponto, no espaço do modelo, para onde a junta do meio
     *               dobra; null mantém o lado da pose atual
     * @param weight 0 = não mexe, 1 = resolve por inteiro (os ângulos de
     *               correção são escalados)
     * @throws IllegalArgumentException se as juntas não formam uma cadeia
     */
    public void solve(Pose pose, int root, int mid, int end, Vector3fc target, Vector3fc pole, float weight) {
        if (!IkJoints.isBelow(pose, mid, root) || !IkJoints.isBelow(pose, end, mid)) {
            throw new IllegalArgumentException("joints " + root + " > " + mid + " > " + end + " are not a chain");
        }
        if (weight <= 0f) {
            pose.computeModelSpace();
            return;
        }
        weight = Math.min(weight, 1f);
        pose.computeModelSpace();
        joints.position(pose, root, a);
        joints.position(pose, mid, b);
        joints.position(pose, end, c);
        t.set(target);

        float lab = b.distance(a);
        float lcb = c.distance(b);
        float distance = t.distance(a);
        if (lab < IkJoints.EPSILON || lcb < IkJoints.EPSILON || distance < IkJoints.EPSILON) {
            return;
        }
        float lat = IkJoints.clamp(distance, Math.abs(lab - lcb) + 1e-4f, lab + lcb - 1e-4f);

        b.sub(a, ab);
        c.sub(a, ac);
        t.sub(a, at);
        a.sub(b, ba);
        c.sub(b, cb);

        // ângulos atuais e desejados do triângulo raiz-meio-ponta
        float acAb0 = IkJoints.angle(ac, ab);
        float baBc0 = IkJoints.angle(ba, cb);
        float acAb1 = (float) Math.acos(IkJoints.clamp((lcb * lcb - lab * lab - lat * lat) / (-2f * lab * lat), -1f, 1f));
        float baBc1 = (float) Math.acos(IkJoints.clamp((lat * lat - lab * lab - lcb * lcb) / (-2f * lab * lcb), -1f, 1f));

        // eixo de flexão: o plano atual; com o braço reto, o do pole (ou qualquer perpendicular)
        bendAxis(pole);

        // 1. dobra raiz e meio para o triângulo ter os ângulos desejados
        joints.rotateInModelSpace(pose, root, axis, (acAb1 - acAb0) * weight, scratch);
        joints.rotateInModelSpace(pose, mid, axis, (baBc1 - baBc0) * weight, scratch);
        pose.computeModelSpace();

        // 2. gira a raiz para a ponta apontar para o alvo
        joints.position(pose, end, c).sub(a, ac);
        if (ac.cross(at, u).lengthSquared() > IkJoints.EPSILON * IkJoints.EPSILON) {
            joints.rotateInModelSpace(pose, root, u.normalize(), IkJoints.angle(ac, at) * weight, scratch);
            pose.computeModelSpace();
        }

        // 3. gira em torno do eixo raiz-alvo para o meio ficar do lado do pole
        if (pole != null) {
            twistTowardPole(pose, root, mid, pole, weight);
        }
    }

    private void bendAxis(Vector3fc pole) {
        ac.cross(ab, axis);
        if (axis.lengthSquared() > IkJoints.EPSILON * IkJoints.EPSILON) {
            axis.normalize();
            return;
        }
        if (pole != null) {
            u.set(pole).sub(a);
            ac.cross(u, axis);
            if (axis.lengthSquared() > IkJoints.EPSILON * IkJoints.EPSILON) {
                // dobra para o pole: o meio sai na direção dele
                axis.normalize().negate();
                return;
            }
        }
        // qualquer perpendicular à cadeia: cruza com X, ou com Y se a cadeia for quase paralela a X
        boolean alongX = Math.abs(ac.x) >= 0.9f * ac.length();
        ac.cross(alongX ? 0f : 1f, alongX ? 1f : 0f, 0f, axis).normalize();
    }

    private void twistTowardPole(Pose pose, int root, int mid, Vector3fc pole, float weight) {
        joints.position(pose, mid, b);
        at.normalize(axis);
        // projeções de (meio - raiz) e (pole - raiz) no plano perpendicular ao eixo raiz-alvo
        b.sub(a, u);
        u.fma(-u.dot(axis), axis);
        v.set(pole).sub(a);
        v.fma(-v.dot(axis), axis);
        if (u.lengthSquared() < IkJoints.EPSILON || v.lengthSquared() < IkJoints.EPSILON) {
            return;
        }
        float angle = IkJoints.angle(u, v);
        if (u.cross(v, side).dot(axis) < 0f) {
            angle = -angle;
        }
        joints.rotateInModelSpace(pose, root, axis, angle * weight, scratch);
        pose.computeModelSpace();
    }
}
