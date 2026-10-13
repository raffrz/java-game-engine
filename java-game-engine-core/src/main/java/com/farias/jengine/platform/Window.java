package com.farias.jengine.platform;

/**
 * Janela do jogo, como os servicos a veem. A implementacao concreta (GLFW,
 * etc.) vem de um modulo de plataforma, que registra este tipo no
 * EngineContext.
 */
public interface Window {

    boolean shouldClose();

    /** Processa os eventos pendentes da janela. */
    void pollEvents();

    /**
     * Liga ou desliga a sincronizacao vertical. Ligada (o padrao), apresentar
     * o frame ({@link Presenter}) espera o refresh do monitor; desligada, o
     * loop roda sem limite de fps.
     */
    void setVSync(boolean enabled);

    /** Largura logica atual da janela (acompanha o redimensionamento). */
    int getWidth();

    /** Altura logica atual da janela; 0 enquanto ela esta minimizada. */
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
