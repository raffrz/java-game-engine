package com.farias.rengine.engine3d;

import com.farias.rengine.renderer.DirectionalLight;
import com.farias.rengine.renderer.Material;
import com.farias.rengine.renderer.Renderer;

/**
 * Fábrica de {@link ShadowMap}. Registrada pelo {@link Engine3DModule}; os
 * recursos de GPU (mapa de profundidade e shader) pertencem ao {@link Renderer}.
 */
public final class Shadows {
    private final Renderer renderer;
    private Material casterMaterial;

    public Shadows(Renderer renderer) {
        this.renderer = renderer;
    }

    /**
     * @param size lado, em texels, do mapa de profundidade quadrado
     */
    public ShadowMap create(DirectionalLight light, int size) {
        if (casterMaterial == null) {
            casterMaterial = renderer.createMaterial(
                    renderer.createShader(Shaders.SHADOW_DEPTH_VERTEX, Shaders.SHADOW_DEPTH_FRAGMENT));
        }
        return new ShadowMap(renderer.createDepthTexture(size, size), light, casterMaterial);
    }
}
