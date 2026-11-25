package com.farias.jengine.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.lwjgl.opengl.GL11.GL_ALWAYS;
import static org.lwjgl.opengl.GL11.GL_GEQUAL;
import static org.lwjgl.opengl.GL11.GL_GREATER;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_LESS;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** A tradução do estado do passe para OpenGL (só constantes, sem contexto GL). */
class OpenGLBackendTest {

    @Test
    void everyDepthCompareHasItsOpenGLFunction() {
        Map<DepthCompare, Integer> expected = Map.of(DepthCompare.LESS, GL_LESS, DepthCompare.LESS_OR_EQUAL, GL_LEQUAL,
                DepthCompare.GREATER, GL_GREATER, DepthCompare.GREATER_OR_EQUAL, GL_GEQUAL, DepthCompare.ALWAYS, GL_ALWAYS);
        assertEquals(DepthCompare.values().length, expected.size(), "toda comparação tem o caso dela");
        expected.forEach((compare, gl) -> assertEquals(gl, OpenGLBackend.depthFunction(compare), compare.name()));
    }
}
