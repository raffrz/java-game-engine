package com.farias.jengine.platform;

/**
 * Apresenta na tela o frame que acabou de ser desenhado (em uma janela,
 * trocar os buffers). E o contrato entre quem desenha e quem tem a
 * superficie: o modulo de plataforma registra a implementacao e o renderer a
 * resolve pelo EngineContext, sem um conhecer o outro.
 */
@FunctionalInterface
public interface Presenter {
    void present();
}
