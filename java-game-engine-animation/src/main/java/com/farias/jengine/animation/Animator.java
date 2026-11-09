package com.farias.jengine.animation;

import java.util.Objects;

/**
 * Toca {@link AnimationClip}s de um {@link Skeleton}: avança o tempo, dá a
 * volta em loop e faz crossfade entre o clip que sai e o que entra. Nada
 * avança sozinho: o jogo chama {@link #update(float)} no próprio loop e lê o
 * resultado em {@link #pose()}.
 *
 * <pre>{@code
 * Animator animator = new Animator(skeleton).play(walk, true);
 * // a cada frame
 * animator.update(deltaTime);
 * model.update(animator.pose());
 * // trocar de clip sem salto
 * animator.crossFade(run, true, 0.3f);
 * }</pre>
 *
 * <p>
 * Durante um crossfade os dois clips avançam e a pose é a mistura deles, com
 * peso linear no tempo. Um crossfade pedido no meio de outro congela a pose
 * misturada daquele instante como origem, sem salto.
 */
public final class Animator {

    private final Pose pose;
    private final Pose from;

    private Slot current;
    private Slot previous;          // clip que sai, ainda avançando
    private boolean fromFrozen;     // origem do crossfade é uma pose congelada em "from"
    private float fadeDuration;
    private float fadeElapsed;
    private float speed = 1f;

    public Animator(Skeleton skeleton) {
        this.pose = new Pose(skeleton);
        this.from = new Pose(skeleton);
    }

    /** Toca {@code clip} do começo, sem transição (corta qualquer crossfade). */
    public Animator play(AnimationClip clip, boolean loop) {
        current = new Slot(Objects.requireNonNull(clip, "clip"), loop);
        endFade();
        evaluate();
        return this;
    }

    /**
     * Passa para {@code clip} misturando com o que está tocando ao longo de
     * {@code seconds}. Sem clip tocando, ou com {@code seconds <= 0}, é o
     * mesmo que {@link #play}.
     */
    public Animator crossFade(AnimationClip clip, boolean loop, float seconds) {
        Objects.requireNonNull(clip, "clip");
        if (current == null || seconds <= 0f) {
            return play(clip, loop);
        }
        if (fading()) {
            from.set(pose);
            fromFrozen = true;
            previous = null;
        } else {
            previous = current;
            fromFrozen = false;
        }
        current = new Slot(clip, loop);
        fadeDuration = seconds;
        fadeElapsed = 0f;
        evaluate();
        return this;
    }

    /** Multiplica o avanço do tempo dos clips: 1 = normal, 0 = pausado, negativo = de trás para frente. */
    public Animator speed(float speed) {
        this.speed = speed;
        return this;
    }

    public float speed() {
        return speed;
    }

    /** O clip tocando (o que entra, durante um crossfade), ou null. */
    public AnimationClip current() {
        return current == null ? null : current.clip;
    }

    /** O tempo do clip atual, em segundos, dentro de [0, duração]. */
    public float time() {
        return current == null ? 0f : current.time;
    }

    /** Pula o clip atual para {@code seconds} (com a volta do loop, ou limitado à duração). */
    public Animator time(float seconds) {
        if (current != null) {
            current.time = 0f;
            current.advance(seconds);
            evaluate();
        }
        return this;
    }

    /** Se há um crossfade em andamento. */
    public boolean fading() {
        return fadeDuration > 0f;
    }

    /** Se o clip atual não está em loop e chegou à ponta para onde o tempo anda. */
    public boolean finished() {
        if (current == null || current.loop) {
            return false;
        }
        return speed >= 0f ? current.time >= current.clip.duration() : current.time <= 0f;
    }

    /**
     * Avança {@code deltaTime} segundos (vezes a velocidade nos clips; o
     * crossfade anda em tempo real) e recalcula a pose.
     */
    public void update(float deltaTime) {
        if (current == null) {
            return;
        }
        float step = deltaTime * speed;
        current.advance(step);
        if (previous != null) {
            previous.advance(step);
        }
        if (fading()) {
            fadeElapsed += deltaTime;
            if (fadeElapsed >= fadeDuration) {
                endFade();
            }
        }
        evaluate();
    }

    /**
     * O resultado do último {@code update}/{@code play}/{@code crossFade}: o
     * repouso se nenhum clip foi tocado. Pode ser lida e copiada; para aplicar
     * IK sem sujar o animator, copie para outra {@link Pose}.
     */
    public Pose pose() {
        return pose;
    }

    private void endFade() {
        previous = null;
        fromFrozen = false;
        fadeDuration = 0f;
        fadeElapsed = 0f;
    }

    private void evaluate() {
        pose.setToRest();
        if (current == null) {
            return;
        }
        current.clip.sample(current.time, pose);
        if (!fading()) {
            return;
        }
        if (!fromFrozen) {
            from.setToRest();
            previous.clip.sample(previous.time, from);
        }
        Poses.blend(from, pose, Math.min(fadeElapsed / fadeDuration, 1f), pose);
    }

    private static final class Slot {
        final AnimationClip clip;
        final boolean loop;
        float time;

        Slot(AnimationClip clip, boolean loop) {
            this.clip = clip;
            this.loop = loop;
        }

        void advance(float step) {
            float duration = clip.duration();
            time += step;
            if (duration <= 0f) {
                time = 0f;
            } else if (loop) {
                time %= duration;
                if (time < 0f) {
                    time += duration;
                }
            } else {
                time = Math.max(0f, Math.min(time, duration));
            }
        }
    }
}
