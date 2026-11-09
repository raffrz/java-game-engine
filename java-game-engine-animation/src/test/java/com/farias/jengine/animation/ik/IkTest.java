package com.farias.jengine.animation.ik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import com.farias.jengine.animation.Pose;
import com.farias.jengine.animation.Skeleton;

class IkTest {

    private static final float EPS = 1e-3f;

    /** Cadeia reta de {@code count} juntas ao longo de +Y, ossos de 1 unidade. */
    private static Skeleton chain(int count) {
        String[] names = new String[count];
        int[] parents = new int[count];
        Vector3f[] t = new Vector3f[count];
        Quaternionf[] r = new Quaternionf[count];
        Vector3f[] s = new Vector3f[count];
        for (int j = 0; j < count; j++) {
            names[j] = "j" + j;
            parents[j] = j - 1;
            t[j] = j == 0 ? new Vector3f() : new Vector3f(0, 1, 0);
            r[j] = new Quaternionf();
            s[j] = new Vector3f(1);
        }
        return Skeleton.ofRestPose(names, parents, t, r, s);
    }

    private static Vector3f at(Pose pose, int joint) {
        pose.computeModelSpace();
        return pose.modelSpace(joint).getTranslation(new Vector3f());
    }

    private static void assertNear(Vector3f expected, Vector3f actual, float eps) {
        assertTrue(expected.distance(actual) < eps, "expected " + expected + " but was " + actual);
    }

    // ---- two-bone ----

    @Test
    void twoBoneReachesTargetAndKeepsLengths() {
        Pose pose = new Pose(chain(3));
        Vector3f target = new Vector3f(1, 1, 0);
        new TwoBoneIk().solve(pose, 0, 1, 2, target, new Vector3f(0, 5, 0), 1f);
        assertNear(target, at(pose, 2), EPS);
        assertEquals(1f, at(pose, 1).distance(at(pose, 0)), EPS);
        assertEquals(1f, at(pose, 2).distance(at(pose, 1)), EPS);
    }

    @Test
    void poleChoosesTheBendSide() {
        Vector3f target = new Vector3f(1, 1, 0);
        // duas soluções: meio em (0, 1, 0) ou (1, 0, 0); o pole escolhe
        Pose up = new Pose(chain(3));
        new TwoBoneIk().solve(up, 0, 1, 2, target, new Vector3f(-1, 5, 0), 1f);
        assertNear(new Vector3f(0, 1, 0), at(up, 1), EPS);

        Pose side = new Pose(chain(3));
        new TwoBoneIk().solve(side, 0, 1, 2, target, new Vector3f(5, -1, 0), 1f);
        assertNear(new Vector3f(1, 0, 0), at(side, 1), EPS);
        assertNear(target, at(side, 2), EPS);
    }

    @Test
    void twoBoneStretchesTowardUnreachableTarget() {
        Pose pose = new Pose(chain(3));
        new TwoBoneIk().solve(pose, 0, 1, 2, new Vector3f(0, 0, 10), new Vector3f(0, 5, 0), 1f);
        assertNear(new Vector3f(0, 0, 2), at(pose, 2), 2e-2f);
    }

    @Test
    void twoBoneFromBentPoseWithoutPole() {
        Pose pose = new Pose(chain(3));
        pose.rotation(1).rotationZ((float) Math.toRadians(60));
        Vector3f target = new Vector3f(-0.5f, 1.2f, 0.4f);
        new TwoBoneIk().solve(pose, 0, 1, 2, target, null, 1f);
        assertNear(target, at(pose, 2), EPS);
    }

    @Test
    void twoBoneWeightZeroKeepsPose() {
        Pose pose = new Pose(chain(3));
        new TwoBoneIk().solve(pose, 0, 1, 2, new Vector3f(1, 1, 0), null, 0f);
        assertNear(new Vector3f(0, 2, 0), at(pose, 2), EPS);
    }

    @Test
    void twoBoneRejectsJointsThatAreNotAChain() {
        assertThrows(IllegalArgumentException.class,
                () -> new TwoBoneIk().solve(new Pose(chain(3)), 1, 0, 2, new Vector3f(), null, 1f));
    }

    // ---- FABRIK ----

    @Test
    void fabrikConvergesAndKeepsLengths() {
        Pose pose = new Pose(chain(5));
        Vector3f target = new Vector3f(2, 2, 1);
        int iterations = new FabrikIk().solve(pose, new int[] { 0, 1, 2, 3, 4 }, target, 20, 1e-4f, 1f);
        assertTrue(iterations > 0 && iterations <= 20, "iterations " + iterations);
        assertNear(target, at(pose, 4), 1e-2f);
        assertNear(new Vector3f(), at(pose, 0), EPS);
        for (int j = 1; j < 5; j++) {
            assertEquals(1f, at(pose, j).distance(at(pose, j - 1)), EPS);
        }
    }

    @Test
    void fabrikUnreachableIsStraightTowardTarget() {
        Pose pose = new Pose(chain(4));
        new FabrikIk().solve(pose, new int[] { 0, 1, 2, 3 }, new Vector3f(10, 0, 0), 10, 1e-4f, 1f);
        assertNear(new Vector3f(1, 0, 0), at(pose, 1), EPS);
        assertNear(new Vector3f(3, 0, 0), at(pose, 3), EPS);
    }

    @Test
    void fabrikRejectsBrokenChain() {
        assertThrows(IllegalArgumentException.class,
                () -> new FabrikIk().solve(new Pose(chain(4)), new int[] { 0, 2, 3 }, new Vector3f(), 5, 1e-3f, 1f));
    }

    // ---- com pais girados (a conversão mundo -> local tem que valer) ----

    /**
     * Junta 0 "armature" girada 45 graus em X e transladada; abaixo dela uma
     * cadeia de 4 juntas com rotações locais quaisquer.
     */
    private static Skeleton twisted() {
        Quaternionf[] r = {
                new Quaternionf().rotationX((float) Math.toRadians(45)),
                new Quaternionf().rotationZ(0.3f),
                new Quaternionf().rotationY(1.1f).rotateX(0.4f),
                new Quaternionf().rotationZ(-0.7f),
                new Quaternionf().rotationXYZ(0.2f, 0.5f, -0.1f) };
        Vector3f[] t = { new Vector3f(3, 1, -2), new Vector3f(0, 0.5f, 0), new Vector3f(0, 1, 0),
                new Vector3f(0, 1.5f, 0), new Vector3f(0, 1, 0) };
        Vector3f[] s = { new Vector3f(1), new Vector3f(1), new Vector3f(1), new Vector3f(1), new Vector3f(1) };
        return Skeleton.ofRestPose(new String[] { "armature", "a", "b", "c", "d" }, new int[] { -1, 0, 1, 2, 3 },
                t, r, s);
    }

    @Test
    void twoBoneUnderRotatedParent() {
        Pose pose = new Pose(twisted());
        Vector3f root = at(pose, 2);
        Vector3f target = new Vector3f(root).add(0.8f, -0.9f, 1.2f);
        Vector3f pole = new Vector3f(root).add(0, 3, 0);
        new TwoBoneIk().solve(pose, 2, 3, 4, target, pole, 1f);
        assertNear(target, at(pose, 4), EPS);
        assertNear(root, at(pose, 2), EPS);
        assertEquals(1.5f, at(pose, 3).distance(at(pose, 2)), EPS);
        assertEquals(1f, at(pose, 4).distance(at(pose, 3)), EPS);
    }

    @Test
    void fabrikUnderRotatedParent() {
        Pose pose = new Pose(twisted());
        Vector3f root = at(pose, 1);
        Vector3f target = new Vector3f(root).add(-1.5f, 1f, 1.5f);
        new FabrikIk().solve(pose, new int[] { 1, 2, 3, 4 }, target, 30, 1e-4f, 1f);
        assertNear(target, at(pose, 4), 1e-2f);
        assertEquals(1.5f, at(pose, 3).distance(at(pose, 2)), EPS);
    }

    @Test
    void lookAtUnderRotatedParent() {
        Pose pose = new Pose(twisted());
        Vector3f joint = at(pose, 3);
        Vector3f target = new Vector3f(joint).add(0, 0, 4);
        new LookAtIk().solve(pose, 3, new Vector3f(0, 1, 0), target, (float) Math.PI, 1f);
        Vector3f bone = at(pose, 4).sub(at(pose, 3)).normalize();
        assertNear(new Vector3f(0, 0, 1), bone, EPS);
    }

    @Test
    void halfWeightMovesPartWay() {
        Pose pose = new Pose(chain(3));
        new LookAtIk().solve(pose, 1, new Vector3f(0, 1, 0), new Vector3f(5, 1, 0), (float) Math.PI, 0.5f);
        Vector3f bone = at(pose, 2).sub(at(pose, 1));
        assertEquals((float) Math.toRadians(45), bone.angle(new Vector3f(0, 1, 0)), EPS);
    }

    // ---- look-at ----

    @Test
    void lookAtPointsTheAxisAtTheTarget() {
        Pose pose = new Pose(chain(3));
        new LookAtIk().solve(pose, 1, new Vector3f(0, 1, 0), new Vector3f(5, 1, 0), (float) Math.PI, 1f);
        // a junta 1 (em 0,1,0) agora aponta para +X: o filho vai para (1, 1, 0)
        assertNear(new Vector3f(1, 1, 0), at(pose, 2), EPS);
    }

    @Test
    void lookAtRespectsMaxAngle() {
        Pose pose = new Pose(chain(3));
        float max = (float) Math.toRadians(30);
        new LookAtIk().solve(pose, 1, new Vector3f(0, 1, 0), new Vector3f(5, 1, 0), max, 1f);
        Vector3f bone = at(pose, 2).sub(at(pose, 1));
        assertEquals(max, bone.angle(new Vector3f(0, 1, 0)), EPS);
    }
}
