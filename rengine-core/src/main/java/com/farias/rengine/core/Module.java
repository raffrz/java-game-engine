package com.farias.rengine.core;

/**
 * Uma peca da engine com ciclo de vida proprio (janela, renderer, assets,
 * ...), registrada em um {@link Engine}. Os modulos iniciam na ordem em que
 * foram registrados e terminam na ordem inversa.
 */
public interface Module {
    /**
     * Chamado por {@link Engine#start()}: resolve do contexto os servicos de
     * que o modulo depende e registra os que ele fornece.
     */
    void onStart(EngineContext context);

    /** Chamado por {@link Engine#terminate()}, so se onStart completou. */
    default void onTerminate(EngineContext context) {
    }
}
