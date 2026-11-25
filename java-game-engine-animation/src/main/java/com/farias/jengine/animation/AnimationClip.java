package com.farias.jengine.animation;

import java.util.List;
import java.util.Objects;

/**
 * Uma animação com nome (andar, correr, acenar): um conjunto de
 * {@link Track}s de um mesmo {@link Skeleton}. Imutável.
 */
public final class AnimationClip {

    private final String name;
    private final List<Track> tracks;
    private final float duration;

    /** A duração é o tempo do último keyframe da trilha mais longa. */
    public AnimationClip(String name, List<Track> tracks) {
        this.name = Objects.requireNonNull(name, "name");
        this.tracks = List.copyOf(tracks);
        float end = 0f;
        for (Track track : this.tracks) {
            end = Math.max(end, track.endTime());
        }
        this.duration = end;
    }

    public String name() {
        return name;
    }

    /** Em segundos. */
    public float duration() {
        return duration;
    }

    public List<Track> tracks() {
        return tracks;
    }

    /**
     * Escreve na pose os canais que o clip anima, no instante {@code time}
     * (sem dar a volta: quem faz loop é o {@link Animator}). Juntas e canais
     * sem trilha não são tocados; para partir do repouso, chame
     * {@link Pose#setToRest()} antes.
     *
     * @throws IndexOutOfBoundsException se uma trilha aponta para uma junta
     *                                   que o esqueleto da pose não tem
     */
    public void sample(float time, Pose pose) {
        for (Track track : tracks) {
            if (track.joint() >= pose.jointCount()) {
                throw new IndexOutOfBoundsException("clip '" + name + "' animates joint " + track.joint()
                        + " but the skeleton has " + pose.jointCount() + " joints");
            }
            track.apply(time, pose);
        }
    }
}
