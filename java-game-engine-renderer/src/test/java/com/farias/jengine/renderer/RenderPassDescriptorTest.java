package com.farias.jengine.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** O estado de um passe, sem contexto GL. */
class RenderPassDescriptorTest {

    @Test
    void aSceneTestsWithLessAndWritesTheDepthByDefault() {
        RenderPassDescriptor scene = new RenderPassDescriptor("scene");
        assertTrue(scene.depthTest());
        assertEquals(DepthCompare.LESS, scene.depthCompare());
        assertTrue(scene.depthWrite());
    }

    @Test
    void aPassCanDrawOnlyWhatIsBehindWithoutWritingTheDepth() {
        RenderPassDescriptor behind = new RenderPassDescriptor("behind");
        assertSame(behind, behind.depthCompare(DepthCompare.GREATER).depthWrite(false));
        assertEquals(DepthCompare.GREATER, behind.depthCompare());
        assertFalse(behind.depthWrite());
    }

    @Test
    void theCompareCannotBeMissing() {
        assertThrows(IllegalArgumentException.class, () -> new RenderPassDescriptor("x").depthCompare(null));
    }
}
