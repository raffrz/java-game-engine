package com.farias.jengine.animation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A hierarquia de juntas de um modelo animado: nome e pai de cada junta, a
 * pose de repouso (translação, rotação e escala locais, relativas ao pai) e a
 * inversa da matriz de bind de cada junta, que leva um vértice do espaço do
 * modelo para o espaço da junta no momento em que a malha foi amarrada.
 *
 * <p>
 * Imutável. As juntas são ordenadas com o pai antes do filho
 * ({@code parent(j) < j}), então percorrer de 0 a {@code jointCount() - 1}
 * já visita a hierarquia de cima para baixo. Pode haver várias raízes
 * ({@code parent(j) == -1}).
 */
public final class Skeleton {

    private final String[] names;
    private final int[] parents;
    private final Vector3f[] restTranslations;
    private final Quaternionf[] restRotations;
    private final Vector3f[] restScales;
    private final Matrix4f[] inverseBinds;
    private final Map<String, Integer> indexByName = new HashMap<>();

    /**
     * Os arrays são copiados. Todos têm o tamanho de {@code names}.
     *
     * @throws IllegalArgumentException se os tamanhos não batem, se uma junta
     *                                  vem antes do pai ou se um nome se repete
     */
    public Skeleton(String[] names, int[] parents, Vector3fc[] restTranslations, Quaternionfc[] restRotations,
            Vector3fc[] restScales, Matrix4fc[] inverseBinds) {
        int count = Objects.requireNonNull(names, "names").length;
        requireLength("parents", parents.length, count);
        requireLength("restTranslations", restTranslations.length, count);
        requireLength("restRotations", restRotations.length, count);
        requireLength("restScales", restScales.length, count);
        requireLength("inverseBinds", inverseBinds.length, count);

        this.names = names.clone();
        this.parents = parents.clone();
        this.restTranslations = new Vector3f[count];
        this.restRotations = new Quaternionf[count];
        this.restScales = new Vector3f[count];
        this.inverseBinds = new Matrix4f[count];
        for (int j = 0; j < count; j++) {
            if (parents[j] < -1 || parents[j] >= j) {
                throw new IllegalArgumentException("joint " + j + " ('" + names[j] + "') has parent " + parents[j]
                        + "; joints must come after their parent");
            }
            if (indexByName.put(Objects.requireNonNull(names[j], "names[" + j + "]"), j) != null) {
                throw new IllegalArgumentException("duplicate joint name '" + names[j] + "'");
            }
            this.restTranslations[j] = new Vector3f(restTranslations[j]);
            this.restRotations[j] = new Quaternionf(restRotations[j]).normalize();
            this.restScales[j] = new Vector3f(restScales[j]);
            this.inverseBinds[j] = new Matrix4f(inverseBinds[j]);
        }
    }

    /**
     * Um esqueleto amarrado na própria pose de repouso: a inversa de bind de
     * cada junta é a inversa da sua matriz de repouso no espaço do modelo.
     * Serve para esqueletos montados em código, sem arquivo.
     */
    public static Skeleton ofRestPose(String[] names, int[] parents, Vector3fc[] restTranslations,
            Quaternionfc[] restRotations, Vector3fc[] restScales) {
        int count = names.length;
        Matrix4f[] model = new Matrix4f[count];
        Matrix4f[] inverseBinds = new Matrix4f[count];
        for (int j = 0; j < count; j++) {
            Matrix4f local = new Matrix4f().translationRotateScale(restTranslations[j], restRotations[j],
                    restScales[j]);
            int parent = parents[j];
            if (parent >= j) {
                throw new IllegalArgumentException("joint " + j + " has parent " + parent
                        + "; joints must come after their parent");
            }
            model[j] = parent < 0 ? local : new Matrix4f(model[parent]).mul(local);
            inverseBinds[j] = new Matrix4f(model[j]).invert();
        }
        return new Skeleton(names, parents, restTranslations, restRotations, restScales, inverseBinds);
    }

    public int jointCount() {
        return names.length;
    }

    /** O pai de {@code joint}, ou -1 se for uma raiz. */
    public int parent(int joint) {
        return parents[joint];
    }

    public String name(int joint) {
        return names[joint];
    }

    /** O índice da junta chamada {@code name}, ou -1 se não existir. */
    public int indexOf(String name) {
        Integer index = indexByName.get(name);
        return index == null ? -1 : index;
    }

    public Vector3fc restTranslation(int joint) {
        return restTranslations[joint];
    }

    public Quaternionfc restRotation(int joint) {
        return restRotations[joint];
    }

    public Vector3fc restScale(int joint) {
        return restScales[joint];
    }

    public Matrix4fc inverseBind(int joint) {
        return inverseBinds[joint];
    }

    private static void requireLength(String name, int length, int expected) {
        if (length != expected) {
            throw new IllegalArgumentException(name + " has " + length + " entries, expected " + expected);
        }
    }
}
