package com.farias.rengine.platform.glfw;

import static org.lwjgl.glfw.GLFW.*;

import com.farias.rengine.io.Key;

/**
 * Traducao Key -> codigo GLFW. Letras, digitos e F1-F12 tem codigos
 * consecutivos no GLFW, entao sao derivados por deslocamento; o resto e
 * explicito. Uma Key sem codigo falha ao carregar a classe.
 */
final class GlfwKeys {
    private static final int[] CODES = new int[Key.values().length];

    static {
        for (char c = 'A'; c <= 'Z'; c++) {
            put(String.valueOf(c), GLFW_KEY_A + (c - 'A'));
        }
        for (int d = 0; d <= 9; d++) {
            put("NUM_" + d, GLFW_KEY_0 + d);
        }
        for (int f = 1; f <= 12; f++) {
            put("F" + f, GLFW_KEY_F1 + (f - 1));
        }

        put("SPACE", GLFW_KEY_SPACE);
        put("ENTER", GLFW_KEY_ENTER);
        put("ESCAPE", GLFW_KEY_ESCAPE);
        put("TAB", GLFW_KEY_TAB);
        put("BACKSPACE", GLFW_KEY_BACKSPACE);

        put("UP", GLFW_KEY_UP);
        put("DOWN", GLFW_KEY_DOWN);
        put("LEFT", GLFW_KEY_LEFT);
        put("RIGHT", GLFW_KEY_RIGHT);

        put("LEFT_SHIFT", GLFW_KEY_LEFT_SHIFT);
        put("RIGHT_SHIFT", GLFW_KEY_RIGHT_SHIFT);
        put("LEFT_CONTROL", GLFW_KEY_LEFT_CONTROL);
        put("RIGHT_CONTROL", GLFW_KEY_RIGHT_CONTROL);
        put("LEFT_ALT", GLFW_KEY_LEFT_ALT);
        put("RIGHT_ALT", GLFW_KEY_RIGHT_ALT);

        for (Key key : Key.values()) {
            if (CODES[key.ordinal()] == 0) {
                throw new ExceptionInInitializerError("No GLFW code for " + key);
            }
        }
    }

    private GlfwKeys() {
    }

    static int code(Key key) {
        return CODES[key.ordinal()];
    }

    private static void put(String keyName, int glfwCode) {
        CODES[Key.valueOf(keyName).ordinal()] = glfwCode;
    }
}
