package com.farias.jengine.examples.city;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.farias.procarch.generation.GeneratedAsset;
import com.farias.procgeo.core.attribute.Attribute;
import com.farias.procgeo.core.attribute.AttributeSet;
import com.farias.procgeo.core.attribute.AttributeType;
import com.farias.procgeo.core.attribute.Domain;
import com.farias.procgeo.core.attribute.MaterialRef;
import com.farias.procgeo.core.geometry.GeometrySet;
import com.farias.procgeo.core.geometry.Mesh;
import com.farias.procgeo.core.math.Quaternion;
import com.farias.procgeo.core.math.Transform3;
import com.farias.procgeo.core.math.Vec3;
import com.farias.procgeo.core.ops.Join;

/**
 * Monta uma cena a partir de assets gerados pela procedural-architecture: posiciona cada asset (translacao + giro em Y),
 * da a ele um tom de cor (quando o asset nao tem um proprio) e junta tudo numa malha so para o renderer.
 */
final class Scene {

    private final List<GeometrySet> pieces = new ArrayList<>();

    /** Asset na posicao (x, z), girado {@code yaw} graus em Y, sem tom proprio. */
    Scene add(GeneratedAsset asset, double x, double z, double yaw) {
        return add(asset.geometry(), x, 0, z, yaw, -1);
    }

    /** Asset na posicao (x, z), girado {@code yaw} graus, com tom de cor {@code tone} (0..1) se ele nao tiver um. */
    Scene add(GeneratedAsset asset, double x, double z, double yaw, double tone) {
        return add(asset.geometry(), x, 0, z, yaw, tone);
    }

    /** Geometria ja gerada (reutilizada varias vezes, como um poste) na posicao (x, y, z). */
    Scene add(GeometrySet geometry, double x, double y, double z, double yaw, double tone) {
        GeometrySet g = tone >= 0 ? withTone(geometry, tone) : geometry;
        Transform3 t = Transform3.trs(new Vec3(x, y, z), Quaternion.fromEulerDegrees(new Vec3(0, yaw, 0)), Vec3.ONE);
        pieces.add(g.transformed(t));
        return this;
    }

    /** Terreno quadrado (material {@code ground}) de lado {@code size}, centrado na origem. */
    Scene ground(double size) {
        double h = size / 2;
        Mesh quad = Mesh.of(List.of(new Vec3(-h, 0, -h), new Vec3(-h, 0, h), new Vec3(h, 0, h), new Vec3(h, 0, -h)),
                List.<int[]>of(new int[] {0, 1, 2, 3}),
                AttributeSet.of(List.of(new Attribute("material", Domain.FACE, AttributeType.MATERIAL, List.of(new MaterialRef("ground"))))));
        pieces.add(GeometrySet.of(quad));
        return this;
    }

    GeometrySet build() {
        return Join.join(pieces);
    }

    private static GeometrySet withTone(GeometrySet g, double tone) {
        Mesh m = g.mesh();
        if (m == null || m.attributes().has("tone")) {
            return g;
        }
        Attribute a = new Attribute("tone", Domain.POINT, AttributeType.FLOAT, Collections.nCopies(m.vertexCount(), (Object) tone));
        return g.with(m.withAttributes(m.attributes().with(a)));
    }
}
