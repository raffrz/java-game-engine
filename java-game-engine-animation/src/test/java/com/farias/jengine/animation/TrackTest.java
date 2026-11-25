package com.farias.jengine.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import com.farias.jengine.animation.Track.Path;

class TrackTest {

    private static final float EPS = 1e-5f;

    private final Skeleton skeleton = TestSkeletons.chain();

    private Vector3f translationAt(Track track, float time) {
        Pose pose = new Pose(skeleton);
        track.apply(time, pose);
        return new Vector3f(pose.translation(track.joint()));
    }

    private Quaternionf rotationAt(Track track, float time) {
        Pose pose = new Pose(skeleton);
        track.apply(time, pose);
        return new Quaternionf(pose.rotation(track.joint()));
    }

    @Test
    void stepHoldsPreviousKeyframe() {
        Track track = new Track(0, Path.TRANSLATION, new float[] { 0, 1 }, new float[] { 0, 0, 0, 10, 0, 0 },
                Interpolation.STEP);
        assertEquals(0f, translationAt(track, 0.99f).x, EPS);
        assertEquals(10f, translationAt(track, 1f).x, EPS);
    }

    @Test
    void linearInterpolatesAndClampsOutside() {
        Track track = new Track(0, Path.TRANSLATION, new float[] { 1, 2, 4 },
                new float[] { 0, 0, 0, 10, 0, 0, 30, 20, 0 }, Interpolation.LINEAR);
        assertEquals(5f, translationAt(track, 1.5f).x, EPS);
        assertEquals(20f, translationAt(track, 3f).x, EPS);
        assertEquals(10f, translationAt(track, 3f).y, EPS);
        assertEquals(0f, translationAt(track, -5f).x, EPS);
        assertEquals(30f, translationAt(track, 99f).x, EPS);
    }

    @Test
    void singleKeyframeIsConstant() {
        Track track = new Track(0, Path.SCALE, new float[] { 0.5f }, new float[] { 2, 3, 4 }, Interpolation.LINEAR);
        Pose pose = new Pose(skeleton);
        track.apply(10f, pose);
        assertEquals(new Vector3f(2, 3, 4), pose.scale(0));
    }

    @Test
    void cubicSplineMatchesHermite() {
        // um eixo: valores 0 -> 1 em [0, 2], tangente de saida do 1o = 1, de entrada do 2o = 0
        float[] values = {
                0, 0, 0, /* in */ 0, 0, 0, /* valor */ 1, 0, 0, /* out */
                0, 0, 0, /* in */ 1, 0, 0, /* valor */ 0, 0, 0 /* out */ };
        Track track = new Track(0, Path.TRANSLATION, new float[] { 0, 2 }, values, Interpolation.CUBICSPLINE);
        // u = 0.5, dt = 2: h00 = .5, h10 = .125, h01 = .5, h11 = -.125
        // p = .5*0 + .125*(1*2) + .5*1 + (-.125)*(0*2) = 0.75
        assertEquals(0.75f, translationAt(track, 1f).x, EPS);
        assertEquals(0f, translationAt(track, 0f).x, EPS);
        assertEquals(1f, translationAt(track, 2f).x, EPS);
    }

    @Test
    void rotationTakesShortestPath() {
        Quaternionf a = new Quaternionf().rotationY((float) Math.toRadians(10));
        // -q de 350 graus: mesma rotacao que -10, mas com o sinal oposto
        Quaternionf b = new Quaternionf().rotationY((float) Math.toRadians(-10));
        b.set(-b.x, -b.y, -b.z, -b.w);
        Track track = new Track(0, Path.ROTATION, new float[] { 0, 1 },
                new float[] { a.x, a.y, a.z, a.w, b.x, b.y, b.z, b.w }, Interpolation.LINEAR);
        Quaternionf middle = rotationAt(track, 0.5f);
        // pelo caminho curto, o meio de +10 e -10 e a identidade (e nao 180 graus)
        assertEquals(0f, middle.angle(), 1e-3f);
    }

    @Test
    void rejectsBadKeyframes() {
        assertThrows(IllegalArgumentException.class, () -> new Track(0, Path.TRANSLATION, new float[] { 1, 1 },
                new float[6], Interpolation.LINEAR));
        assertThrows(IllegalArgumentException.class, () -> new Track(0, Path.ROTATION, new float[] { 0, 1 },
                new float[6], Interpolation.LINEAR));
        assertThrows(IllegalArgumentException.class, () -> new Track(0, Path.TRANSLATION, new float[] { 0 },
                new float[3], Interpolation.CUBICSPLINE));
        assertThrows(IllegalArgumentException.class, () -> new Track(0, Path.TRANSLATION, new float[0],
                new float[0], Interpolation.LINEAR));
    }

    @Test
    void clipDurationAndUntouchedJoints() {
        Track move = new Track(1, Path.TRANSLATION, new float[] { 0, 3 }, new float[] { 0, 1, 0, 0, 4, 0 },
                Interpolation.LINEAR);
        AnimationClip clip = new AnimationClip("move", List.of(move));
        assertEquals(3f, clip.duration(), EPS);

        Pose pose = new Pose(skeleton);
        pose.translation(2).set(9, 9, 9);
        clip.sample(1.5f, pose);
        assertEquals(2.5f, pose.translation(1).y, EPS);
        assertEquals(new Vector3f(9, 9, 9), pose.translation(2));
    }

    @Test
    void clipRejectsJointOutsideSkeleton() {
        Track track = new Track(7, Path.TRANSLATION, new float[] { 0 }, new float[3], Interpolation.STEP);
        AnimationClip clip = new AnimationClip("bad", List.of(track));
        assertThrows(IndexOutOfBoundsException.class, () -> clip.sample(0f, new Pose(skeleton)));
    }
}
