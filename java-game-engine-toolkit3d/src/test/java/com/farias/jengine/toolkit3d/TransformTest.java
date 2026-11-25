package com.farias.jengine.toolkit3d;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** A matriz de modelo comum a {@link TexturedModel} e {@link SkinnedModel}, sem contexto GL. */
class TransformTest {

    @Test
    void matrixIsTranslationTimesRotationXyzTimesScale() {
        Transform transform = new Transform();
        transform.position().set(1, 2, 3);
        transform.rotation().set(10, 20, 30);
        transform.scale().set(2, 1, 0.5f);

        Matrix4f expected = new Matrix4f().translation(1, 2, 3)
                .mul(new Matrix4f().rotationXYZ((float) Math.toRadians(10), (float) Math.toRadians(20),
                        (float) Math.toRadians(30)))
                .mul(new Matrix4f().scaling(2, 1, 0.5f));
        assertTrue(expected.equals(transform.matrix(), 1e-5f), transform.matrix().toString());
    }

    @Test
    void modelsShareTheConvention() {
        TexturedModel model = new TexturedModel(null, null);
        model.getPosition().set(4, 0, -1);
        model.getRotation().set(0, 90, 0);
        model.getScale().set(3);
        assertTrue(model.transform().matrix().equals(model.getModelMatrix(), 0f));
        // a escala vem antes da rotação: x local escalado por 3 e girado 90 graus em Y vai para -Z
        Vector3f p = model.getModelMatrix().transformPosition(new Vector3f(1, 0, 0));
        assertTrue(p.equals(new Vector3f(4, 0, -4), 1e-5f), p.toString());
    }
}
