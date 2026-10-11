package com.farias.rengine.engine.modules;

import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Module;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Window;

/**
 * Cria o InputSystem para a janela ja registrada por {@link WindowModule}.
 */
public final class InputModule implements Module {
    @Override
    public void onRegister(EngineContext context) {
        Window window = context.resolve(Window.class);
        InputSystem inputSystem = new InputSystem(window);
        context.registerService(InputSystem.class, inputSystem);
    }
}
