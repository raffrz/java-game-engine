package com.farias.rengine.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * Registro de servicos compartilhado entre os modulos de um {@link Engine}.
 * Cada modulo registra o que cria (Window, InputSystem, Renderer, ...) e
 * qualquer outro modulo ou o jogo em si pode resolver pelo tipo.
 */
public final class EngineContext {
    private final Map<Class<?>, Object> services = new HashMap<>();

    public void registerService(Class<?> clazz, Object service) {
        services.put(clazz, service);
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> clazz) {
        return (T) services.get(clazz);
    }
}
