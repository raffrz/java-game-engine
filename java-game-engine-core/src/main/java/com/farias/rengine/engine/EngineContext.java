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

    /**
     * Retorna o servico registrado para o tipo, ou null se nao houver.
     */
    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> clazz) {
        return (T) services.get(clazz);
    }

    /**
     * Retorna o servico registrado para o tipo ou falha com uma mensagem
     * clara. Um modulo que depende de outro chama isto no inicio de
     * onRegister, assim registrar os modulos na ordem errada quebra ali, e
     * nao mais tarde com um NullPointerException ou um erro de contexto GL.
     */
    public <T> T require(Class<T> clazz) {
        T service = resolve(clazz);
        if (service == null) {
            throw new IllegalStateException("Required service not registered: " + clazz.getSimpleName()
                    + ". Register the module that provides it before the module that needs it.");
        }
        return service;
    }
}
