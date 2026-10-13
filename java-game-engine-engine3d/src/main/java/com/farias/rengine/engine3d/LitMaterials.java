package com.farias.rengine.engine3d;

import com.farias.rengine.renderer.Renderer;
import com.farias.rengine.renderer.Shader;
import com.farias.rengine.renderer.Texture;

/**
 * Fabrica de {@link LitMaterial}, o material padrao do toolkit 3D. O shader e
 * criado na primeira chamada, compartilhado por todos os materiais e pertence
 * ao {@link Renderer}.
 */
public final class LitMaterials {
    private final Renderer renderer;
    private Shader shader;

    LitMaterials(Renderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Material para {@code texture}: fosco, sem sombra e com a luz padrao.
     * Refine com {@link LitMaterial#light}, {@link LitMaterial#glossy} e
     * {@link LitMaterial#receiving}.
     */
    public LitMaterial create(Texture texture) {
        if (shader == null) {
            shader = renderer.createShader(Shaders.STANDARD_VERTEX, Shaders.STANDARD_FRAGMENT);
        }
        return new LitMaterial(shader, texture);
    }
}
