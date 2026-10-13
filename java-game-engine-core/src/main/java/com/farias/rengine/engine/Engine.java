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
    private final List<Module> started = new ArrayList<>();
    private final EngineContext context = new EngineContext();
    private boolean running;

    public Engine registerModule(Module module) {
        if (running) {
            throw new IllegalStateException("registerModule called after start()");
        }
        modules.add(module);
        return this;
    }

    /**
     * Chama onRegister de cada modulo, na ordem registrada, e retorna o
     * contexto com todos os servicos disponiveis. Se um modulo falhar, os que
     * ja tinham iniciado sao encerrados (ordem inversa) antes de a excecao
     * sair, entao quem chama nao precisa de terminate() nesse caso.
     */
    public EngineContext start() {
        if (running) {
            throw new IllegalStateException("start() called twice");
        }
        running = true;
        try {
            for (Module module : modules) {
                module.onRegister(context);
                started.add(module);
            }
        } catch (RuntimeException | Error e) {
            try {
                terminate();
            } catch (RuntimeException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
        return context;
    }

    /**
     * Encerra os modulos iniciados, na ordem inversa do registro. Chamar de
     * novo (ou sem start()) nao faz nada.
     */
    public void terminate() {
        for (int i = started.size() - 1; i >= 0; i--) {
            started.remove(i).onTerminate(context);
        }
        running = false;
    }

    public EngineContext context() {
        return context;
    }
}
