package com.farias.jengine.examples.chess;

import com.farias.jengine.toolkit3d.IsometricCamera;
import com.farias.jengine.platform.Input;
import com.farias.jengine.platform.Key;
import com.farias.jengine.platform.MouseButton;

/**
 * Traduz teclado e mouse em movimento da câmera: setas movem, a roda do mouse
 * aproxima e afasta, arrastar com o botão do meio gira.
 */
final class ChessCameraControls {

    private static final float SPEED = 5.0f;
    private static final float PAN_SPEED = 3.5f;

    /** Quanto o enquadramento muda por clique da roda do mouse. */
    private static final float ZOOM_PER_NOTCH = 0.5f;

    /**
     * Graus de rotação por pixel arrastado com o botão do meio. Não é por
     * segundo: o arrasto já é o deslocamento do frame, sem deltaTime.
     */
    private static final float ROTATE_SENSITIVITY = 0.4f;

    private final Input input;
    private final IsometricCamera camera;

    /** Posição X do mouse no frame anterior, para calcular o arrasto. */
    private double lastMouseX;

    ChessCameraControls(Input input, IsometricCamera camera) {
        this.input = input;
        this.camera = camera;
    }

    void update(float deltaTime) {
        camera.pan(axis(Key.RIGHT, Key.LEFT) * PAN_SPEED * deltaTime,
                axis(Key.UP, Key.DOWN) * SPEED * deltaTime);

        // rolar para cima (positivo) aproxima, ou seja, diminui o enquadramento
        camera.zoom((float) -input.getScrollY() * ZOOM_PER_NOTCH);

        handleRotation();
    }

    /**
     * Arrastar na horizontal com o botão do meio gira a câmera. O Input só
     * dá a posição do mouse, não o deslocamento, então ele é calculado aqui
     * a partir de lastMouseX.
     */
    private void handleRotation() {
        double mouseX = input.getMouseX();
        if (input.isMouseButtonPressed(MouseButton.MIDDLE)) {
            // início do arrasto: só marca a posição, para não saltar
            lastMouseX = mouseX;
        } else if (input.isMouseButtonDown(MouseButton.MIDDLE)) {
            camera.rotate((float) (mouseX - lastMouseX) * ROTATE_SENSITIVITY);
            lastMouseX = mouseX;
        }
    }

    private float axis(Key positive, Key negative) {
        return (input.isKeyDown(positive) ? 1f : 0f) - (input.isKeyDown(negative) ? 1f : 0f);
    }
}
