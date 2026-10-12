package com.farias.rengine.platform.glfw;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.DoubleBuffer;

import org.lwjgl.BufferUtils;

import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Key;
import com.farias.rengine.io.MouseButton;

/** Teclado e mouse via GLFW, para a janela dada. */
final class GlfwInput implements InputSystem {
    private static final int KEY_FIRST = GLFW_KEY_SPACE;
    private static final int KEY_LAST = GLFW_KEY_LAST;

    private final GlfwWindow window;
    private final long handle;

    private final boolean[] keys = new boolean[KEY_LAST];
    private final boolean[] mouseButtons = new boolean[MouseButton.values().length];

    private final DoubleBuffer mouseX = BufferUtils.createDoubleBuffer(1);
    private final DoubleBuffer mouseY = BufferUtils.createDoubleBuffer(1);

    GlfwInput(GlfwWindow window) {
        this.window = window;
        this.handle = window.handle();
    }

    @Override
    public boolean isKeyDown(Key key) {
        return isDown(GlfwKeys.code(key));
    }

    @Override
    public boolean isKeyPressed(Key key) {
        int code = GlfwKeys.code(key);
        return isDown(code) && !keys[code];
    }

    @Override
    public boolean isKeyReleased(Key key) {
        int code = GlfwKeys.code(key);
        return !isDown(code) && keys[code];
    }

    @Override
    public boolean isMouseButtonDown(MouseButton button) {
        return glfwGetMouseButton(handle, glfwButton(button)) == GLFW_PRESS;
    }

    @Override
    public boolean isMouseButtonPressed(MouseButton button) {
        return isMouseButtonDown(button) && !mouseButtons[button.ordinal()];
    }

    @Override
    public double getMouseX() {
        return mouseX.get(0);
    }

    @Override
    public double getMouseY() {
        return mouseY.get(0);
    }

    @Override
    public void update() {
        for (int i = KEY_FIRST; i < KEY_LAST; i++) {
            keys[i] = isDown(i);
        }

        for (MouseButton button : MouseButton.values()) {
            mouseButtons[button.ordinal()] = isMouseButtonDown(button);
        }

        glfwGetCursorPos(handle, mouseX, mouseY);

        window.pollEvents();
    }

    private boolean isDown(int glfwKey) {
        return glfwGetKey(handle, glfwKey) == GLFW_PRESS;
    }

    private static int glfwButton(MouseButton button) {
        return switch (button) {
            case LEFT -> GLFW_MOUSE_BUTTON_LEFT;
            case RIGHT -> GLFW_MOUSE_BUTTON_RIGHT;
            case MIDDLE -> GLFW_MOUSE_BUTTON_MIDDLE;
        };
    }
}
