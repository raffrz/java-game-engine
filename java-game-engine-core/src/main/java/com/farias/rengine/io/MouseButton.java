package com.farias.rengine.io;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Botoes de mouse reconhecidos pelo {@link InputSystem}, com o codigo GLFW
 * correspondente.
 */
public enum MouseButton {
    LEFT(GLFW_MOUSE_BUTTON_LEFT),
    RIGHT(GLFW_MOUSE_BUTTON_RIGHT),
    MIDDLE(GLFW_MOUSE_BUTTON_MIDDLE);

    private final int glfwCode;

    MouseButton(int glfwCode) {
        this.glfwCode = glfwCode;
    }

    public int getGlfwCode() {
        return glfwCode;
    }
}
