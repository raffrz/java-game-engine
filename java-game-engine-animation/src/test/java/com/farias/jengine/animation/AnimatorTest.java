package com.farias.jengine.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import com.farias.jengine.animation.Track.Path;

class AnimatorTest {

    private static final float EPS = 1e-4f;

    private final Skeleton skeleton = TestSkeletons.chain();

    /** Translada a raiz em x de {@code from} a {@code to} em {@code seconds}. */
    private static AnimationClip slide(String name, float from, float to, float seconds) {
        return new AnimationClip(name, List.of(new Track(0, Path.TRANSLATION, new float[] { 0, seconds },
                new float[] { from, 0, 0, to, 0, 0 }, Interpolation.LINEAR)));
    }

    /** Raiz parada em x = {@code x}. */
    private static AnimationClip hold(String name, float x) {
        return slide(name, x, x, 1f);
    }

    private float rootX(Animator animator) {
        return animator.pose().translation(0).x;
    }

    @Test
    void restPoseWithoutClip() {
        Animator animator = new Animator(skeleton);
        animator.update(1f);
        assertNull(animator.current());
        assertEquals(new Vector3f(0, 1, 0), animator.pose().translation(1));
    }

    @Test
    void loopWrapsAround() {
        Animator animator = new Animator(skeleton).play(slide("s", 0, 10, 2), true);
        animator.update(2.5f);
        assertEquals(0.5f, animator.time(), EPS);
        assertEquals(2.5f, rootX(animator), EPS);
        assertFalse(animator.finished());
    }

    @Test
    void nonLoopStopsAtEndAndFinishes() {
        Animator animator = new Animator(skeleton).play(slide("s", 0, 10, 2), false);
        animator.update(1f);
        assertFalse(animator.finished());
        animator.update(5f);
        assertTrue(animator.finished());
        assertEquals(10f, rootX(animator), EPS);
    }

    @Test
    void negativeSpeedPlaysBackwards() {
        Animator animator = new Animator(skeleton).play(slide("s", 0, 10, 2), true).speed(-1f);
        animator.update(0.5f);
        assertEquals(1.5f, animator.time(), EPS);
        assertEquals(7.5f, rootX(animator), EPS);
    }

    @Test
    void crossFadeGoesFromOldToNew() {
        Animator animator = new Animator(skeleton).play(hold("a", 0), true);
        animator.crossFade(hold("b", 10), true, 1f);
        assertTrue(animator.fading());
        assertEquals(0f, rootX(animator), EPS);

        animator.update(0.25f);
        assertEquals(2.5f, rootX(animator), EPS);

        animator.update(0.75f);
        assertFalse(animator.fading());
        assertEquals(10f, rootX(animator), EPS);
        assertEquals("b", animator.current().name());
    }

    @Test
    void crossFadeDuringCrossFadeDoesNotJump() {
        Animator animator = new Animator(skeleton).play(hold("a", 0), true);
        animator.crossFade(hold("b", 10), true, 1f);
        animator.update(0.5f);
        float before = rootX(animator);
        assertEquals(5f, before, EPS);

        animator.crossFade(hold("c", -10), true, 1f);
        assertEquals(before, rootX(animator), EPS);

        animator.update(0.5f);
        assertEquals(-2.5f, rootX(animator), EPS);
    }

    @Test
    void timeSetterWrapsAndSamples() {
        Animator animator = new Animator(skeleton).play(slide("s", 0, 10, 2), true);
        animator.time(3f);
        assertEquals(1f, animator.time(), EPS);
        assertEquals(5f, rootX(animator), EPS);
    }

    @Test
    void blendHalfwayAndAliasing() {
        Pose a = new Pose(skeleton);
        Pose b = new Pose(skeleton);
        b.translation(0).set(4, 0, 0);
        b.rotation(0).rotationY((float) Math.toRadians(90));

        Poses.blend(a, b, 0.5f, b);
        assertEquals(2f, b.translation(0).x, EPS);
        assertEquals((float) Math.toRadians(45), b.rotation(0).angle(), EPS);

        Poses.blend(a, b, 0f, a);
        assertEquals(new Quaternionf(), a.rotation(0));
    }
}
