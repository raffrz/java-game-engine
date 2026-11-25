package com.farias.jengine.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class SkeletonAndPoseTest {

    private static final float EPS = 1e-5f;

    @Test
    void rejectsJointBeforeItsParent() {
        Vector3f[] t = { new Vector3f(), new Vector3f() };
        Quaternionf[] r = { new Quaternionf(), new Quaternionf() };
        Vector3f[] s = { new Vector3f(1), new Vector3f(1) };
        Matrix4f[] ib = { new Matrix4f(), new Matrix4f() };
        assertThrows(IllegalArgumentException.class,
                () -> new Skeleton(new String[] { "a", "b" }, new int[] { 1, -1 }, t, r, s, ib));
        assertThrows(IllegalArgumentException.class,
                () -> new Skeleton(new String[] { "a", "a" }, new int[] { -1, 0 }, t, r, s, ib));
        assertThrows(IllegalArgumentException.class,
                () -> new Skeleton(new String[] { "a" }, new int[] { -1, 0 }, t, r, s, ib));
    }

    @Test
    void findsJointsByName() {
        Skeleton skeleton = TestSkeletons.chain();
        assertEquals(3, skeleton.jointCount());
        assertEquals(1, skeleton.indexOf("mid"));
        assertEquals(-1, skeleton.indexOf("nope"));
        assertEquals(0, skeleton.parent(1));
        assertEquals(-1, skeleton.parent(0));
    }

    @Test
    void paletteIsIdentityAtRest() {
        Pose pose = new Pose(TestSkeletons.chain());
        float[] palette = new float[Pose.FLOATS_PER_JOINT * 3];
        pose.skinningPalette(palette);
        Matrix4f joint = new Matrix4f();
        for (int j = 0; j < 3; j++) {
            joint.set(palette, j * Pose.FLOATS_PER_JOINT);
            assertTrue(joint.equals(new Matrix4f(), EPS), "joint " + j + " = " + joint);
        }
    }

    @Test
    void modelSpaceComposesParentFirst() {
        Pose pose = new Pose(TestSkeletons.chain());
        // gira a raiz 90 graus em Z: a cadeia que apontava para +Y passa a apontar para -X
        pose.rotation(0).rotationZ((float) Math.toRadians(90));
        pose.computeModelSpace();

        Vector3f tip = pose.modelSpace(2).getTranslation(new Vector3f());
        assertEquals(-2f, tip.x, EPS);
        assertEquals(0f, tip.y, EPS);

        // o palette leva o vértice da ponta, no bind, para a nova posição
        float[] palette = new float[Pose.FLOATS_PER_JOINT * 3];
        pose.skinningPalette(palette);
        Vector3f moved = new Matrix4f().set(palette, 2 * Pose.FLOATS_PER_JOINT)
                .transformPosition(new Vector3f(0, 2, 0));
        assertEquals(-2f, moved.x, EPS);
        assertEquals(0f, moved.y, EPS);
    }

    @Test
    void setToRestUndoesChanges() {
        Pose pose = new Pose(TestSkeletons.chain());
        pose.translation(1).set(5, 5, 5);
        pose.rotation(1).rotationX(1f);
        pose.setToRest();
        assertEquals(new Vector3f(0, 1, 0), pose.translation(1));
        assertEquals(new Quaternionf(), pose.rotation(1));
    }

    @Test
    void rejectsPaletteTooSmall() {
        Pose pose = new Pose(TestSkeletons.chain());
        assertThrows(IllegalArgumentException.class, () -> pose.skinningPalette(new float[16]));
    }

    @Test
    void copyRequiresSameSkeleton() {
        Pose a = new Pose(TestSkeletons.chain());
        Pose b = new Pose(TestSkeletons.chain());
        assertThrows(IllegalArgumentException.class, () -> a.set(b));
    }
}
