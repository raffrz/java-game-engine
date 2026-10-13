package com.farias.jengine.platform;

/**
 * Estado de teclado e mouse, como os servicos o veem. A implementacao
 * concreta vem de um modulo de plataforma.
 *
 * <p>As consultas respondem sobre a amostra feita em {@link #update()}, entao
 * o estado nao muda no meio de um frame. Por frame:
 * {@code window.pollEvents(); input.update();}.
 */
public interface Input {

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

    /**
     * Amostra o estado atual de teclas, botoes e cursor. Uma vez por frame,
     * depois de {@code Window.pollEvents()} (este metodo nao processa eventos).
     */
    void update();
}
