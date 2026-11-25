package com.farias.jengine.examples.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.farias.jengine.animation.Skeleton;
import com.farias.jengine.renderer.MeshData;

/**
 * Um tentáculo gerado em código: um esqueleto reto de {@code segments} ossos
 * ao longo de +Y e um cilindro afinando com skin, cada anel preso ao osso em
 * que está (e misturado com o de baixo perto da junta, para dobrar suave).
 * Mostra que {@code Skeleton.ofRestPose} + {@code MeshData} com juntas e pesos
 * bastam para animar algo sem arquivo.
 */
final class Tentacle {

    private static final int SIDES = 12;
    private static final int RINGS_PER_SEGMENT = 4;

    private Tentacle() {
    }

    /** {@code segments + 1} juntas: a base em y = 0 e uma a cada {@code length}; a última é a ponta. */
    static Skeleton skeleton(int segments, float length) {
        int count = segments + 1;
        String[] names = new String[count];
        int[] parents = new int[count];
        Vector3f[] translations = new Vector3f[count];
        Quaternionf[] rotations = new Quaternionf[count];
        Vector3f[] scales = new Vector3f[count];
        for (int j = 0; j < count; j++) {
            names[j] = j == segments ? "tip" : "segment" + j;
            parents[j] = j - 1;
            translations[j] = j == 0 ? new Vector3f() : new Vector3f(0, length, 0);
            rotations[j] = new Quaternionf();
            scales[j] = new Vector3f(1);
        }
        return Skeleton.ofRestPose(names, parents, translations, rotations, scales);
    }

    /** As juntas da base à ponta, para o FABRIK. */
    static int[] chain(int segments) {
        int[] chain = new int[segments + 1];
        for (int j = 0; j <= segments; j++) {
            chain[j] = j;
        }
        return chain;
    }

    /** Cilindro com raio de {@code baseRadius} a {@code tipRadius}, no espaço de bind do esqueleto. */
    static MeshData mesh(int segments, float length, float baseRadius, float tipRadius) {
        int rings = segments * RINGS_PER_SEGMENT + 1;
        int columns = SIDES + 1;
        int vertexCount = rings * columns;
        float[] vertices = new float[vertexCount * 3];
        float[] uvs = new float[vertexCount * 2];
        float[] normals = new float[vertexCount * 3];
        int[] joints = new int[vertexCount * 4];
        float[] weights = new float[vertexCount * 4];
        float height = segments * length;

        for (int ring = 0; ring < rings; ring++) {
            float along = (float) ring / (rings - 1);
            float y = along * height;
            float radius = baseRadius + (tipRadius - baseRadius) * along;
            // osso em que o anel está e mistura com o de baixo na metade inferior
            float t = y / length;
            int bone = Math.min((int) t, segments - 1);
            float fraction = t - bone;
            for (int side = 0; side < columns; side++) {
                double angle = 2 * Math.PI * side / SIDES;
                float x = (float) Math.cos(angle);
                float z = (float) Math.sin(angle);
                int v = ring * columns + side;
                vertices[v * 3] = x * radius;
                vertices[v * 3 + 1] = y;
                vertices[v * 3 + 2] = z * radius;
                normals[v * 3] = x;
                normals[v * 3 + 2] = z;
                uvs[v * 2] = (float) side / SIDES;
                uvs[v * 2 + 1] = along;
                if (fraction < 0.5f && bone > 0) {
                    joints[v * 4] = bone;
                    weights[v * 4] = 0.5f + fraction;
                    joints[v * 4 + 1] = bone - 1;
                    weights[v * 4 + 1] = 0.5f - fraction;
                } else {
                    joints[v * 4] = bone;
                    weights[v * 4] = 1f;
                }
            }
        }

        int[] indices = new int[(rings - 1) * SIDES * 6];
        int i = 0;
        for (int ring = 0; ring < rings - 1; ring++) {
            for (int side = 0; side < SIDES; side++) {
                int a = ring * columns + side;
                int b = a + columns;
                indices[i++] = a;
                indices[i++] = b;
                indices[i++] = a + 1;
                indices[i++] = a + 1;
                indices[i++] = b;
                indices[i++] = b + 1;
            }
        }
        return new MeshData(vertices, uvs, normals, indices, joints, weights);
    }
}
