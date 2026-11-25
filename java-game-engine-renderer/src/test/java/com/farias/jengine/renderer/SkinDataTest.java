package com.farias.jengine.renderer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

/** Validação dos dados de skin, sem contexto GL. */
class SkinDataTest {

    private static final float[] TRIANGLE = { 0, 0, 0, 1, 0, 0, 0, 1, 0 };
    private static final float[] UVS = new float[6];
    private static final int[] INDICES = { 0, 1, 2 };

    @Test
    void staticMeshHasNoSkin() {
        MeshData data = new MeshData(TRIANGLE, UVS, TRIANGLE, INDICES);
        assertFalse(data.skinned());
        assertNull(data.joints());
    }

    @Test
    void skinNeedsFourInfluencesPerVertex() {
        MeshData data = new MeshData(TRIANGLE, UVS, TRIANGLE, INDICES, new int[12], new float[12]);
        assertTrue(data.skinned());
        assertThrows(IllegalArgumentException.class,
                () -> new MeshData(TRIANGLE, UVS, TRIANGLE, INDICES, new int[9], new float[9]));
        assertThrows(IllegalArgumentException.class,
                () -> new MeshData(TRIANGLE, UVS, TRIANGLE, INDICES, new int[12], new float[8]));
    }

    @Test
    void jointsAndWeightsComeTogether() {
        assertThrows(IllegalArgumentException.class,
                () -> new MeshData(TRIANGLE, UVS, TRIANGLE, INDICES, new int[12], null));
        assertThrows(IllegalArgumentException.class,
                () -> new MeshData(TRIANGLE, UVS, TRIANGLE, INDICES, null, new float[12]));
    }

    @Test
    void paletteMustHoldWholeMatrices() {
        assertNull(new RenderCommand(null, null, new Matrix4f()).jointPalette());
        assertDoesNotThrow(() -> new RenderCommand(null, null, new Matrix4f(), new float[32]));
        assertThrows(IllegalArgumentException.class,
                () -> new RenderCommand(null, null, new Matrix4f(), new float[20]));
    }
}
