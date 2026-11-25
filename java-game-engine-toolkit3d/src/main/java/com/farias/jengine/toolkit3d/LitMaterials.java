package com.farias.jengine.toolkit3d;

import com.farias.jengine.renderer.Renderer;
import com.farias.jengine.renderer.Shader;
import com.farias.jengine.renderer.Texture;

/**
 * Fabrica de {@link LitMaterial}, o material padrao do toolkit 3D. O shader e
 * criado na primeira chamada, compartilhado por todos os materiais e pertence
 * ao {@link Renderer}.
 */
public final class LitMaterials implements SkinnedMaterials {
    private final Renderer renderer;
    private Shader shader;
    private Shader skinnedShader;

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

    /**
     * Idem, para malhas com skin ({@link SkinnedModel}): mesma iluminação e
     * os mesmos refinamentos, com a malha deformada pelo esqueleto. Não serve
     * para malha sem skin (todos os vértices iriam para a origem).
     */
    @Override
    public LitMaterial createSkinned(Texture texture) {
        if (skinnedShader == null) {
            skinnedShader = renderer.createShader(Shaders.SKINNED_VERTEX, Shaders.STANDARD_FRAGMENT);
        }
        return new LitMaterial(skinnedShader, texture);
    }
}
