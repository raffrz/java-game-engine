package com.farias.rengine.engine3d;

import com.farias.rengine.renderer.Mesh;
import com.farias.rengine.renderer.Renderer;

/**
 * Fabrica de Mesh para as formas basicas de {@link ShapeData}. Cada chamada
 * cria um Mesh novo na GPU (do {@link Renderer}); guarde-o e reutilize.
 */
public final class Shapes {
    private final Renderer renderer;

    Shapes(Renderer renderer) {
        this.renderer = renderer;
    }

    /** Ver {@link ShapeData#plane}. */
    public Mesh plane(float width, float height) {
        return renderer.createMesh(ShapeData.plane(width, height));
    }

    /** Ver {@link ShapeData#cube}. */
    public Mesh cube() {
        return renderer.createMesh(ShapeData.cube());
    }

    /** Ver {@link ShapeData#floor}. */
    public Mesh floor() {
        return renderer.createMesh(ShapeData.floor());
    }
}
