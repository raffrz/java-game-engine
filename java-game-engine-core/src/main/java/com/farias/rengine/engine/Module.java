package com.farias.rengine.engine;

/**
 * Um servico com um ciclo de vida proprio (janela, input, renderer, ...),
 * registrado em um {@link Engine} e inicializado/terminado na ordem em que
 * foi registrado.
 */
public interface Module {
    void onRegister(EngineContext context);

    default void onTerminate(EngineContext context) {
    }
}
