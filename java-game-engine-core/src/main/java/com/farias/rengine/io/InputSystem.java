package com.farias.rengine.io;

/**
 * Estado de teclado e mouse, como os servicos o veem. A implementacao
 * concreta vem de um modulo de plataforma.
 */
public interface InputSystem {

    boolean isKeyDown(Key key);

    /** true no frame em que a tecla passou de solta para pressionada. */
    boolean isKeyPressed(Key key);

    /** true no frame em que a tecla passou de pressionada para solta. */
    boolean isKeyReleased(Key key);

    boolean isMouseButtonDown(MouseButton button);

    /** true no frame em que o botao passou de solto para pressionado. */
    boolean isMouseButtonPressed(MouseButton button);

    double getMouseX();

    double getMouseY();

    /** Amostra o estado atual e processa os eventos da janela. Uma vez por frame. */
    void update();
}
