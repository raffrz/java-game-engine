package com.farias.jengine.toolkit3d;

import com.farias.jengine.renderer.Material;
import com.farias.jengine.renderer.Renderer;

/**
 * Fábrica de {@link ShadowMap}. Registrada pelo {@link Toolkit3DModule}; os
 * recursos de GPU (mapa de profundidade e shader) pertencem ao {@link Renderer}.
 */
public final class Shadows {
    private static final float DEFAULT_BIAS = 0.004f;

    private final Renderer renderer;
    private Material casterMaterial;

    Shadows(Renderer renderer) {
        this.renderer = renderer;
    }

    /**
     * @param size lado, em texels, do mapa de profundidade quadrado
     */
    public ShadowMap create(DirectionalLight light, int size) {
        return create(light, size, DEFAULT_BIAS);
    }

    /**
     * @param size lado, em texels, do mapa de profundidade quadrado
     * @param bias tolerância de profundidade contra o serrilhado da sombra
     *             (acne); fixa por mapa, porque cada material que recebe a
     *             sombra a copia em {@link LitMaterial#receiving}
     */
    public ShadowMap create(DirectionalLight light, int size, float bias) {
        if (casterMaterial == null) {
            casterMaterial = renderer.createMaterial(
                    renderer.createShader(Shaders.SHADOW_DEPTH_VERTEX, Shaders.SHADOW_DEPTH_FRAGMENT));
        }
        return new ShadowMap(renderer.createDepthTexture(size, size), light, casterMaterial, bias);
    }
}
