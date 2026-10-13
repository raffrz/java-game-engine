package com.farias.rengine.renderer;

public record ClearColor(float r, float g, float b, float a) {
    public static ClearColor black() {
        return new ClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    }

    public static ClearColor white() {
        return new ClearColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static ClearColor gray() {
        return new ClearColor(0.5f, 0.5f, 0.5f, 1.0f);
    }
}