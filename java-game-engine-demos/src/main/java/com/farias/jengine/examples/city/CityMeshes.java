package com.farias.jengine.examples.city;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.farias.jengine.renderer.MeshData;
import com.farias.procgeo.core.attribute.Attribute;
import com.farias.procgeo.core.attribute.MaterialRef;
import com.farias.procgeo.core.geometry.GeometrySet;
import com.farias.procgeo.core.geometry.Mesh;
import com.farias.procgeo.core.math.Vec3;
import com.farias.procgeo.core.mesh.Triangulator;

/**
 * Adaptador entre a biblioteca procedural-geometry e o renderer: converte a malha
 * (faces poligonais, atributos) em {@link MeshData} triangulado, um por material.
 * Normais por canto ({@link Mesh#cornerNormals}): faces marcadas com o atributo FACE {@code smooth} se misturam com as
 * vizinhas dentro de {@link #CREASE_DEGREES} (superficies curvas lisas); as demais ficam chapadas. O atributo de
 * vertice {@code tone} vira a coordenada U, que escolhe a cor numa paleta (ver {@link CityPalettes}).
 */
final class CityMeshes {

    static final String DEFAULT_MATERIAL = "wall";

    /** Angulo maximo entre faces "suaves" (atributo {@code smooth}) para as normais serem misturadas. */
    static final double CREASE_DEGREES = 50.0;

    private CityMeshes() {
    }

    static Map<String, MeshData> byMaterial(GeometrySet geometry, Map<String, Integer> paletteSizes) {
        Mesh mesh = geometry.mesh();
        Map<String, Builder> builders = new LinkedHashMap<>();
        if (mesh == null) {
            return Map.of();
        }
        Attribute material = mesh.attributes().get("material");
        Attribute tone = mesh.attributes().get("tone");
        Attribute smoothAttribute = mesh.attributes().get("smooth");
        boolean[] smooth = new boolean[mesh.faceCount()];
        if (smoothAttribute != null) {
            for (int f = 0; f < smooth.length; f++) {
                smooth[f] = (Boolean) smoothAttribute.get(f);
            }
        }
        Vec3[] cornerNormals = mesh.cornerNormals(CREASE_DEGREES, smooth);
        List<Integer> triangle = new ArrayList<>();

        for (int f = 0; f < mesh.faceCount(); f++) {
            String name = material == null ? "" : ((MaterialRef) material.get(f)).id();
            if (name.isEmpty()) {
                name = DEFAULT_MATERIAL;
            }
            triangle.clear();
            Triangulator.triangulateFace(mesh, f, triangle);
            if (triangle.isEmpty()) {
                continue;
            }
            int paletteSize = paletteSizes.getOrDefault(name, 1);
            Builder builder = builders.computeIfAbsent(name, k -> new Builder());
            for (int vertex : triangle) {
                double t = tone == null ? 0.5 : (Double) tone.get(vertex);
                builder.add(mesh.position(vertex), cornerNormal(mesh, cornerNormals, f, vertex), paletteU(t, paletteSize));
            }
        }

        Map<String, MeshData> result = new LinkedHashMap<>();
        builders.forEach((name, builder) -> result.put(name, builder.build()));
        return result;
    }

    /** Normal do canto de {@code face} que usa {@code vertex}. */
    private static Vec3 cornerNormal(Mesh mesh, Vec3[] cornerNormals, int face, int vertex) {
        for (int c = mesh.faceStart(face); c < mesh.faceStart(face) + mesh.faceSize(face); c++) {
            if (mesh.cornerVertex(c) == vertex) {
                return cornerNormals[c];
            }
        }
        return mesh.faceNormal(face);
    }

    /** Centro do texel escolhido por {@code tone} em [0, 1) numa paleta de {@code size} cores. */
    private static float paletteU(double tone, int size) {
        int index = Math.min(size - 1, Math.max(0, (int) Math.floor(tone * size)));
        return (index + 0.5f) / size;
    }

    private static final class Builder {
        private final List<Float> vertices = new ArrayList<>();
        private final List<Float> textures = new ArrayList<>();
        private final List<Float> normals = new ArrayList<>();

        void add(Vec3 p, Vec3 n, float u) {
            vertices.add((float) p.x());
            vertices.add((float) p.y());
            vertices.add((float) p.z());
            normals.add((float) n.x());
            normals.add((float) n.y());
            normals.add((float) n.z());
            textures.add(u);
            textures.add(0.5f);
        }

        MeshData build() {
            int count = vertices.size() / 3;
            int[] indices = new int[count];
            for (int i = 0; i < count; i++) {
                indices[i] = i;
            }
            return new MeshData(toArray(vertices), toArray(textures), toArray(normals), indices);
        }

        private static float[] toArray(List<Float> list) {
            float[] a = new float[list.size()];
            for (int i = 0; i < a.length; i++) {
                a[i] = list.get(i);
            }
            return a;
        }
    }
}
