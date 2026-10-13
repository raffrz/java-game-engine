package com.farias.jengine.examples.chess;

import com.farias.jengine.toolkit3d.IsometricCamera;
import com.farias.jengine.platform.Input;
import com.farias.jengine.platform.Key;

/** Traduz o teclado em movimento da câmera: setas movem, Z aproxima, Shift+Z afasta. */
final class ChessCameraControls {

    private static final float SPEED = 5.0f;
    private static final float PAN_SPEED = 3.5f;
    private static final float ZOOM_SPEED = 2f * SPEED;

    private final Input input;
    private final IsometricCamera camera;

    ChessCameraControls(Input input, IsometricCamera camera) {
        this.input = input;
        this.camera = camera;
    }

    void update(float deltaTime) {
        camera.pan(axis(Key.RIGHT, Key.LEFT) * PAN_SPEED * deltaTime,
                axis(Key.UP, Key.DOWN) * SPEED * deltaTime);

        if (input.isKeyDown(Key.Z)) {
            float direction = input.isKeyDown(Key.LEFT_SHIFT) ? 1f : -1f;
            camera.zoom(direction * ZOOM_SPEED * deltaTime);
        }
    }

    private float axis(Key positive, Key negative) {
        return (input.isKeyDown(positive) ? 1f : 0f) - (input.isKeyDown(negative) ? 1f : 0f);
    }
}
