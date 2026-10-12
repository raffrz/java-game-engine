package com.farias.rengine.io;

/**
 * Janela do jogo, como os servicos a veem. A implementacao concreta (GLFW,
 * etc.) vem de um modulo de plataforma, que registra este tipo no
 * EngineContext.
 */
public interface Window {

    boolean shouldClose();

    /** Processa os eventos pendentes da janela. */
    void pollEvents();

    /** Apresenta o frame (troca os buffers). */
    void swapBuffers();

    int getWidth();

    int getHeight();

    /**
     * Tamanho real do framebuffer em pixels. Em telas HiDPI (ex: Retina no
     * macOS) isso difere de getWidth/getHeight (tamanho logico), e e o que
     * deve ser usado para configurar o viewport.
     */
    int getFramebufferWidth();

    int getFramebufferHeight();

    void setTitle(String title);
}
