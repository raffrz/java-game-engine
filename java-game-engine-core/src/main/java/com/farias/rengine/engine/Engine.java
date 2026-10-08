package com.farias.rengine.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Orquestra o registro e o encerramento de {@link Module}s. E uma biblioteca,
 * nao um framework: quem possui o game loop e quem chama este Engine, nao o
 * contrario (o jogo controla o loop; o Engine so fornece servicos).
 */
public final class Engine {
    private final List<Module> modules = new ArrayList<>();
    private final EngineContext context = new EngineContext();

    public Engine registerModule(Module module) {
        modules.add(module);
        return this;
    }

    /**
     * Chama onRegister de cada modulo, na ordem registrada, e retorna o
     * contexto com todos os servicos disponiveis.
     */
    public EngineContext start() {
        for (Module module : modules) {
            module.onRegister(context);
        }
        return context;
    }

    /**
     * Encerra os modulos na ordem inversa do registro.
     */
    public void terminate() {
        for (int i = modules.size() - 1; i >= 0; i--) {
            modules.get(i).onTerminate(context);
        }
    }

    public EngineContext context() {
        return context;
    }
}
