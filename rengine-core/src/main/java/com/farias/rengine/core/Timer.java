package com.farias.rengine.core;

/**
 * Cronometro simples de deltaTime entre chamadas a {@link #update()}.
 */
public final class Timer {
    private long lastUpdateTime;
    private float deltaTime;

    public Timer() {
        lastUpdateTime = System.nanoTime();
    }

    public void update() {
        long now = System.nanoTime();
        deltaTime = (now - lastUpdateTime) / 1_000_000_000f;
        lastUpdateTime = now;
    }

    public float deltaTime() {
        return deltaTime;
    }
}
