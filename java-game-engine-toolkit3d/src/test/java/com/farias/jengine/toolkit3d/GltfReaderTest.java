package com.farias.jengine.toolkit3d;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import com.farias.jengine.animation.AnimationClip;
import com.farias.jengine.animation.Pose;
import com.farias.jengine.animation.Skeleton;
import com.farias.jengine.renderer.MeshData;

class GltfReaderTest {

    private static final float EPS = 1e-4f;

    private static final Function<String, byte[]> NO_FILES = p -> {
        throw new AssertionError("unexpected file read: " + p);
    };

    private static GltfData embedded() {
        return GltfReader.read("models/test.gltf", TestGltf.embedded(), NO_FILES);
    }

    @Test
    void skeletonHasEveryNeededNodeParentFirst() {
        Skeleton skeleton = embedded().skeleton();
        assertEquals(5, skeleton.jointCount());
        String[] names = new String[5];
        int[] parents = new int[5];
        for (int j = 0; j < 5; j++) {
            names[j] = skeleton.name(j);
            parents[j] = skeleton.parent(j);
        }
        assertArrayEquals(new String[] { "body", "Armature", "base", "tip", "hat" }, names);
        assertArrayEquals(new int[] { -1, -1, 1, 2, 3 }, parents);
    }

    @Test
    void skinnedMeshRemapsJointsAndRenormalizesWeights() {
        MeshData mesh = embedded().primitives().get(0).mesh();
        int base = 2;
        int tip = 3;
        assertEquals(base, mesh.joints()[0]);
        assertEquals(1f, mesh.weights()[0], EPS);
        // vértice 1: metade base, metade tip (128 + 128 de 255 renormalizado)
        assertEquals(base, mesh.joints()[4]);
        assertEquals(tip, mesh.joints()[5]);
        assertEquals(0.5f, mesh.weights()[4], EPS);
        assertEquals(0.5f, mesh.weights()[5], EPS);
        assertEquals(tip, mesh.joints()[8]);
        // posições como no arquivo (a translação do nó com skin é ignorada), normal do stride intercalado
        assertArrayEquals(new float[] { 0, 0, 0, 1, 0, 0, 0, 2, 0 }, mesh.vertices(), EPS);
        assertArrayEquals(new float[] { 0, 0, 1, 0, 0, 1, 0, 0, 1 }, mesh.normals(), EPS);
        // UV sem inversão
        assertArrayEquals(new float[] { 0, 0, 1, 0, 0, 1 }, mesh.textures(), EPS);
        assertArrayEquals(new int[] { 0, 1, 2 }, mesh.indices());
    }

    @Test
    void restPoseReproducesTheFile() {
        GltfData data = embedded();
        float[] palette = new float[Pose.FLOATS_PER_JOINT * data.skeleton().jointCount()];
        new Pose(data.skeleton()).skinningPalette(palette);

        // malha com skin: no repouso, os vértices ficam onde o arquivo diz
        MeshData body = data.primitives().get(0).mesh();
        assertEquals(new Vector3f(0, 2, 0), skin(body, 2, palette));
        assertEquals(new Vector3f(1, 0, 0), skin(body, 1, palette));

        // malha rígida: na posição do nó no mundo (escala 2, translações 1 + 0.5)
        MeshData hat = data.primitives().get(1).mesh();
        assertEquals(new Vector3f(0, 3, 0), skin(hat, 0, palette));
        assertEquals(new Vector3f(2, 3, 0), skin(hat, 1, palette));
    }

    @Test
    void animationMovesRigidPartsWithTheirJoint() {
        GltfData data = embedded();
        AnimationClip wave = data.clips().get(0);
        assertEquals("wave", wave.name());
        assertEquals(1f, wave.duration(), EPS);
        assertEquals(2, wave.tracks().size());

        Pose pose = new Pose(data.skeleton());
        wave.sample(1f, pose);
        pose.translation(1).set(0, 0, 0); // tira a translação do Armature para isolar a rotação
        float[] palette = new float[Pose.FLOATS_PER_JOINT * data.skeleton().jointCount()];
        pose.skinningPalette(palette);

        // tip gira 90 em Z: o chapéu (0.5 acima da tip) vai para 0.5 à esquerda, em escala 2
        Vector3f hat = skin(data.primitives().get(1).mesh(), 0, palette);
        assertEquals(-1f, hat.x, EPS);
        assertEquals(2f, hat.y, EPS);
    }

    @Test
    void nonJointAncestorAnimationIsKept() {
        GltfData data = embedded();
        Pose pose = new Pose(data.skeleton());
        data.clips().get(0).sample(0f, pose);
        assertEquals(new Vector3f(5, 0, 0), pose.translation(1));
    }

    @Test
    void missingNormalsAreComputedAndBaked() {
        MeshData hat = embedded().primitives().get(1).mesh();
        assertArrayEquals(new float[] { 0, -1, 0, 0, -1, 0, 0, -1, 0 }, hat.normals(), EPS);
        assertArrayEquals(new int[] { 0, 1, 2 }, hat.indices());
    }

    @Test
    void materialAndWarnings() {
        GltfData data = embedded();
        assertNull(data.primitives().get(0).image());
        assertArrayEquals(new float[] { 1, 0, 0, 1 }, data.primitives().get(0).baseColor(), EPS);
        assertTrue(data.warnings().stream().anyMatch(w -> w.contains("weights")), data.warnings().toString());
    }

    @Test
    void sameDataFromExternalBinAndGlb() {
        GltfData embedded = embedded();
        GltfData external = GltfReader.read("models/test.gltf", TestGltf.external("test%20data.bin"), path -> {
            assertEquals("models/test data.bin", path);
            return TestGltf.bin();
        });
        GltfData glb = GltfReader.read("models/test.glb", TestGltf.glb(), NO_FILES);
        for (GltfData other : new GltfData[] { external, glb }) {
            assertEquals(embedded.skeleton().jointCount(), other.skeleton().jointCount());
            for (int p = 0; p < 2; p++) {
                MeshData a = embedded.primitives().get(p).mesh();
                MeshData b = other.primitives().get(p).mesh();
                assertArrayEquals(a.vertices(), b.vertices(), EPS);
                assertArrayEquals(a.normals(), b.normals(), EPS);
                assertArrayEquals(a.joints(), b.joints());
                assertArrayEquals(a.weights(), b.weights(), EPS);
            }
            assertEquals(embedded.clips().get(0).tracks().size(), other.clips().get(0).tracks().size());
        }
    }

    @Test
    void rejectsRequiredExtensions() {
        String json = "{\"asset\": {\"version\": \"2.0\"}, \"extensionsRequired\": [\"KHR_draco_mesh_compression\"]}";
        assertThrows(UnsupportedOperationException.class,
                () -> GltfReader.read("x.gltf", json.getBytes(StandardCharsets.UTF_8), NO_FILES));
    }

    /** Vértice {@code v} da malha deformado pelo palette, como no vertex shader. */
    private static Vector3f skin(MeshData mesh, int v, float[] palette) {
        Vector3f position = new Vector3f(mesh.vertices()[v * 3], mesh.vertices()[v * 3 + 1], mesh.vertices()[v * 3 + 2]);
        Vector3f out = new Vector3f();
        Matrix4f joint = new Matrix4f();
        for (int i = 0; i < 4; i++) {
            float weight = mesh.weights()[v * 4 + i];
            if (weight == 0f) {
                continue;
            }
            joint.set(palette, mesh.joints()[v * 4 + i] * Pose.FLOATS_PER_JOINT);
            out.add(joint.transformPosition(position, new Vector3f()).mul(weight));
        }
        // arredonda para comparar com equals sem ruído de float
        return out.set(Math.round(out.x * 1e4f) / 1e4f, Math.round(out.y * 1e4f) / 1e4f, Math.round(out.z * 1e4f) / 1e4f);
    }
}
