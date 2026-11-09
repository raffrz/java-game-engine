package com.farias.jengine.animation.ik;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.farias.jengine.animation.Pose;

/**
 * IK de cadeia de N ossos por FABRIK (Forward And Backward Reaching Inverse
 * Kinematics): ajusta as posições das juntas indo da ponta para a raiz e de
 * volta, preservando os comprimentos, até a ponta chegar ao alvo; depois
 * converte as posições em rotações. Bom para caudas, tentáculos, colunas e
 * pescoços. Sem limites angulares por junta.
 *
 * <p>
 * Trabalha no espaço do modelo e escreve rotações locais na pose. A raiz da
 * cadeia não sai do lugar. Reaproveite a instância (tem scratch interno; não é
 * thread-safe).
 */
public final class FabrikIk {

    private final IkJoints joints = new IkJoints();
    private final Vector3f current = new Vector3f();
    private final Vector3f desired = new Vector3f();
    private final Vector3f direction = new Vector3f();
    private final Quaternionf delta = new Quaternionf();
    private final Quaternionf identity = new Quaternionf();
    private Vector3f[] positions = new Vector3f[0];
    private float[] lengths = new float[0];

    /**
     * Leva a última junta de {@code chain} até {@code target}.
     *
     * @param pose          a pose a corrigir (o espaço do modelo é
     *                      recalculado)
     * @param chain         juntas da raiz à ponta, cada uma filha direta da
     *                      anterior; pelo menos 2
     * @param target        alvo da ponta, no espaço do modelo
     * @param maxIterations limite de idas e voltas
     * @param tolerance     distância ponta-alvo que basta
     * @param weight        0 = não mexe, 1 = resolve por inteiro (cada
     *                      rotação de correção é interpolada)
     * @return as iterações usadas (0 se já estava no alvo ou fora de alcance)
     * @throws IllegalArgumentException se {@code chain} não é uma cadeia de
     *                                  pai para filho
     */
    public int solve(Pose pose, int[] chain, Vector3fc target, int maxIterations, float tolerance, float weight) {
        int n = chain.length;
        if (n < 2) {
            throw new IllegalArgumentException("a chain needs at least 2 joints");
        }
        for (int i = 1; i < n; i++) {
            if (pose.skeleton().parent(chain[i]) != chain[i - 1]) {
                throw new IllegalArgumentException("joint " + chain[i] + " is not a child of " + chain[i - 1]);
            }
        }
        ensureCapacity(n);
        pose.computeModelSpace();
        if (weight <= 0f) {
            return 0;
        }
        weight = Math.min(weight, 1f);

        float total = 0f;
        for (int i = 0; i < n; i++) {
            joints.position(pose, chain[i], positions[i]);
            if (i > 0) {
                lengths[i - 1] = positions[i].distance(positions[i - 1]);
                total += lengths[i - 1];
            }
        }

        Vector3f root = positions[0];
        int iterations = 0;
        if (root.distance(target.x(), target.y(), target.z()) >= total) {
            // fora de alcance: cadeia reta apontando para o alvo
            direction.set(target).sub(root);
            if (direction.lengthSquared() < IkJoints.EPSILON) {
                return 0;
            }
            direction.normalize();
            for (int i = 1; i < n; i++) {
                positions[i].set(direction).mul(lengths[i - 1]).add(positions[i - 1]);
            }
        } else {
            current.set(root);
            while (iterations < maxIterations
                    && positions[n - 1].distance(target.x(), target.y(), target.z()) > tolerance) {
                // da ponta para a raiz: a ponta vai ao alvo
                positions[n - 1].set(target);
                for (int i = n - 2; i >= 0; i--) {
                    place(positions[i], positions[i + 1], lengths[i]);
                }
                // da raiz para a ponta: a raiz volta ao lugar
                positions[0].set(current);
                for (int i = 1; i < n; i++) {
                    place(positions[i], positions[i - 1], lengths[i - 1]);
                }
                iterations++;
            }
        }

        // posições -> rotações: cada junta gira para o filho apontar para a nova posição
        for (int i = 0; i < n - 1; i++) {
            joints.position(pose, chain[i], current);
            joints.position(pose, chain[i + 1], direction).sub(current);
            desired.set(positions[i + 1]).sub(current);
            if (direction.lengthSquared() < IkJoints.EPSILON || desired.lengthSquared() < IkJoints.EPSILON) {
                continue;
            }
            delta.rotationTo(direction, desired);
            if (weight < 1f) {
                identity.identity().slerp(delta, weight, delta);
            }
            joints.rotateInModelSpace(pose, chain[i], delta);
            pose.computeModelSpace();
        }
        return iterations;
    }

    /** Põe {@code point} a {@code length} de {@code anchor}, na direção em que já estava. */
    private void place(Vector3f point, Vector3f anchor, float length) {
        direction.set(point).sub(anchor);
        if (direction.lengthSquared() < IkJoints.EPSILON * IkJoints.EPSILON) {
            direction.set(0f, 1f, 0f);
        }
        point.set(direction.normalize().mul(length)).add(anchor);
    }

    private void ensureCapacity(int n) {
        if (positions.length < n) {
            positions = new Vector3f[n];
            for (int i = 0; i < n; i++) {
                positions[i] = new Vector3f();
            }
            lengths = new float[n];
        }
    }
}
