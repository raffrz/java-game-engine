package com.farias.jengine.platform.glfw;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.DoubleBuffer;

import org.lwjgl.BufferUtils;

import com.farias.jengine.platform.Input;
import com.farias.jengine.platform.Key;
import com.farias.jengine.platform.MouseButton;

/**
 * Teclado e mouse via GLFW, para a janela dada. Guarda a amostra do frame
 * atual e a do anterior; "pressionada"/"solta" sao a diferenca entre as duas.
 *
 * <p>A rolagem nao tem estado para consultar no GLFW, so evento: o callback
 * acumula o que chega em {@code pollEvents()} e {@link #update()} entrega o
 * acumulado como a rolagem do frame.
 */
final class GlfwInput implements Input {
    private static final int KEY_FIRST = GLFW_KEY_SPACE;
    private static final int KEY_LAST = GLFW_KEY_LAST;
    private static final int BUTTON_COUNT = MouseButton.values().length;

    private final long handle;

    private boolean[] keys = new boolean[KEY_LAST + 1];
    private boolean[] previousKeys = new boolean[KEY_LAST + 1];
    private boolean[] mouseButtons = new boolean[BUTTON_COUNT];
    private boolean[] previousMouseButtons = new boolean[BUTTON_COUNT];

    private final DoubleBuffer mouseX = BufferUtils.createDoubleBuffer(1);
    private final DoubleBuffer mouseY = BufferUtils.createDoubleBuffer(1);

    private double pendingScrollY;
    private double scrollY;

    GlfwInput(GlfwWindow window) {
        this.handle = window.handle();
        glfwSetScrollCallback(handle, (w, dx, dy) -> pendingScrollY += dy);
    }

    @Override
    public boolean isKeyDown(Key key) {
        return keys[GlfwKeys.code(key)];
    }

    @Override
    public boolean isKeyPressed(Key key) {
        int code = GlfwKeys.code(key);
        return keys[code] && !previousKeys[code];
    }

    @Override
    public boolean isKeyReleased(Key key) {
        int code = GlfwKeys.code(key);
        return !keys[code] && previousKeys[code];
    }

    @Override
    public boolean isMouseButtonDown(MouseButton button) {
        return mouseButtons[button.ordinal()];
    }

    @Override
    public boolean isMouseButtonPressed(MouseButton button) {
        return mouseButtons[button.ordinal()] && !previousMouseButtons[button.ordinal()];
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
    public double getScrollY() {
        return scrollY;
    }

    @Override
    public void update() {
        boolean[] swap = previousKeys;
        previousKeys = keys;
        keys = swap;
        for (int i = KEY_FIRST; i <= KEY_LAST; i++) {
            keys[i] = glfwGetKey(handle, i) == GLFW_PRESS;
        }

        swap = previousMouseButtons;
        previousMouseButtons = mouseButtons;
        mouseButtons = swap;
        for (MouseButton button : MouseButton.values()) {
            mouseButtons[button.ordinal()] = glfwGetMouseButton(handle, glfwButton(button)) == GLFW_PRESS;
        }

        glfwGetCursorPos(handle, mouseX, mouseY);

        scrollY = pendingScrollY;
        pendingScrollY = 0;
    }

    private static int glfwButton(MouseButton button) {
        return switch (button) {
            case LEFT -> GLFW_MOUSE_BUTTON_LEFT;
            case RIGHT -> GLFW_MOUSE_BUTTON_RIGHT;
            case MIDDLE -> GLFW_MOUSE_BUTTON_MIDDLE;
        };
    }
}
